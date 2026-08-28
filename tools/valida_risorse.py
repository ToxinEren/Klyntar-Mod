"""Validatore delle risorse della mod, pensato per girare come hook PostToolUse.

Legge il payload dell'hook su stdin, ricava il file toccato e, se e' un json sotto
src/main/resources, controlla quattro cose:

  1. che il json sia sintatticamente valido;
  2. che ogni texture citata come "<namespace>:textures/....png" esista su disco;
  3. che in un file di potere ogni "power" nomini il potere stesso, e che i render layer
     che cita lo nominino a loro volta;
  4. che nei file *.animation.json i valori dei keyframe siano vettori e non numeri nudi
     (un numero nudo fa crashare GeckoLib con "Not a JSON Object: 1").

In caso di problemi stampa su stdout un json con decision/block, cosi' l'errore torna
subito a Claude invece di manifestarsi come crash dopo due minuti di runClient.
"""
import collections
import json
import os
import re
import sys

RIF_TEXTURE = re.compile(r'"([a-z0-9_.-]+):(textures/[A-Za-z0-9_/.-]+\.png)"')
RIF_POTERE = re.compile(r'"power"\s*:\s*"([a-z0-9_.-]+):([A-Za-z0-9_/.-]+)"')
RIF_LAYER = re.compile(r'"render_layer"\s*:\s*"([a-z0-9_.-]+):([A-Za-z0-9_/.-]+)"')
CANALI = ("scale", "rotation", "position")


def esci_pulito():
    sys.exit(0)


def radice_risorse(percorso):
    """risale dal file fino alla cartella src/main/resources che lo contiene"""
    corrente = os.path.dirname(os.path.abspath(percorso))
    while True:
        genitore, nome = os.path.split(corrente)
        if genitore == corrente:
            return None
        if nome == "resources" and genitore.replace("\\", "/").endswith("src/main"):
            return corrente
        corrente = genitore


def controlla_texture(testo, risorse):
    """ogni "<ns>:textures/...png" deve esistere sotto assets/<ns>/"""
    mancanti = []
    for ns, rel in set(RIF_TEXTURE.findall(testo)):
        base = os.path.join(risorse, "assets", ns)
        if not os.path.isdir(base):
            continue                       # namespace non nostro (es. minecraft), non verificabile
        if not os.path.exists(os.path.join(base, rel)):
            mancanti.append("%s:%s" % (ns, rel))
    return sorted(mancanti)


def controlla_id_potere(testo, risorse, potere):
    """
    In un file di potere ogni "power" deve nominare il potere stesso.

    E' il difetto piu' ricorrente del progetto: un potere derivato da un altro si porta dietro
    l'id di partenza, e le condizioni che lo citano falliscono in silenzio invece di dare errore.
    """
    sbagliati = collections.Counter()
    for ns, pid in RIF_POTERE.findall(testo):
        if pid != potere:
            sbagliati["%s:%s" % (ns, pid)] += 1
    return ["%s citato %d volte" % (rif, n) for rif, n in sorted(sbagliati.items())]


def controlla_layer(testo, risorse, potere):
    """
    Un render layer lega il potere con un campo singolo, non con una condizione: se il layer
    citato da questo potere non lo nomina mai, la variabile non si risolve e la maschera resta
    ferma a zero. E' il motivo per cui i layer di una forma derivata vanno duplicati.

    Un layer che nomina anche altre forme va bene: i compound raggruppano di proposito le
    varianti di piu' poteri. Il difetto e' che manchi la propria.
    """
    guasti = []
    for ns, lid in sorted(set(RIF_LAYER.findall(testo))):
        p = os.path.join(risorse, "assets", ns, "palladium/render_layers", lid + ".json")
        if not os.path.exists(p):
            guasti.append("il layer %s:%s non esiste" % (ns, lid))
            continue
        try:
            corpo = open(p, "rb").read().decode("utf-8-sig")
        except Exception:
            continue
        citati = {pid for _, pid in RIF_POTERE.findall(corpo)}
        if citati and potere not in citati:
            guasti.append("il layer %s:%s non nomina mai questo potere, solo %s"
                          % (ns, lid, ", ".join(sorted(citati))))
    return guasti


def controlla_keyframe(dati):
    """nelle animazioni GeckoLib un valore di keyframe deve essere un vettore"""
    guasti = []
    animazioni = dati.get("animations")
    if not isinstance(animazioni, dict):
        return guasti
    for nome_anim, anim in animazioni.items():
        ossa = anim.get("bones") if isinstance(anim, dict) else None
        if not isinstance(ossa, dict):
            continue
        for nome_osso, osso in ossa.items():
            if not isinstance(osso, dict):
                continue
            for canale in CANALI:
                valore = osso.get(canale)
                if not isinstance(valore, dict):
                    continue               # numero o lista costante: legittimo
                for istante, v in valore.items():
                    if isinstance(v, (int, float)):
                        guasti.append("%s / %s / %s @ %s = %s"
                                      % (nome_anim, nome_osso, canale, istante, v))
                    elif isinstance(v, dict) and not ({"vector", "pre", "post"} & set(v)):
                        guasti.append("%s / %s / %s @ %s: manca 'vector'"
                                      % (nome_anim, nome_osso, canale, istante))
    return guasti


def main():
    try:
        payload = json.load(sys.stdin)
    except Exception:
        esci_pulito()

    ingresso = payload.get("tool_input") or {}
    risposta = payload.get("tool_response") or {}
    percorso = risposta.get("filePath") or ingresso.get("file_path") or ""
    if not percorso.endswith(".json") or not os.path.isfile(percorso):
        esci_pulito()

    risorse = radice_risorse(percorso)
    if risorse is None:
        esci_pulito()

    try:
        grezzo = open(percorso, "rb").read()
        testo = grezzo.decode("utf-8-sig")   # molti file del pack hanno il BOM, e va bene cosi'
    except Exception:
        esci_pulito()

    nome = os.path.basename(percorso)
    problemi = []

    try:
        dati = json.loads(testo)
    except ValueError as e:
        problemi.append("%s non e' un JSON valido: %s" % (nome, e))
        dati = None

    if dati is not None:
        mancanti = controlla_texture(testo, risorse)
        if mancanti:
            problemi.append("%s cita texture inesistenti: %s" % (nome, ", ".join(mancanti)))

        # i file di potere hanno due controlli in piu', sui riferimenti incrociati
        cartella = os.path.dirname(percorso).replace("\\", "/")
        if cartella.endswith("palladium/powers"):
            potere = nome[:-5]
            sbagliati = controlla_id_potere(testo, risorse, potere)
            if sbagliati:
                problemi.append("%s nomina altri poteri (un id ereditato da una copia?): %s"
                                % (nome, "; ".join(sbagliati)))
            guasti = controlla_layer(testo, risorse, potere)
            if guasti:
                problemi.append("%s: %s" % (nome, "; ".join(guasti)))

        if nome.endswith(".animation.json"):
            guasti = controlla_keyframe(dati)
            if guasti:
                problemi.append("%s ha keyframe scalari dove serve un vettore (GeckoLib "
                                "crashera' con \"Not a JSON Object\"): %s"
                                % (nome, "; ".join(guasti[:8])))

    if problemi:
        print(json.dumps({
            "decision": "block",
            "reason": "Validazione risorse fallita.\n- " + "\n- ".join(problemi),
            "systemMessage": "Validazione risorse: %d problema/i in %s" % (len(problemi), nome),
        }))
    esci_pulito()


if __name__ == "__main__":
    main()
