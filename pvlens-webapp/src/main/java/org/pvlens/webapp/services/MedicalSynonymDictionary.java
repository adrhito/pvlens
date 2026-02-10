package org.pvlens.webapp.services;

import java.util.*;

/**
 * Comprehensive medical synonym dictionary for mapping common/lay terms to medical terminology.
 * This dictionary enables users to search using everyday language and find relevant MedDRA terms.
 *
 * Coverage includes:
 * - Pain and discomfort (all body regions)
 * - Gastrointestinal symptoms
 * - Cardiovascular symptoms
 * - Respiratory symptoms
 * - Neurological symptoms
 * - Psychiatric/mental health
 * - Dermatological symptoms
 * - Musculoskeletal issues
 * - Urological/renal symptoms
 * - Reproductive/gynecological symptoms
 * - Ophthalmological symptoms
 * - ENT (Ear, Nose, Throat) symptoms
 * - General/constitutional symptoms
 * - Allergic/immunological symptoms
 * - British vs American spelling variations
 */
public class MedicalSynonymDictionary {

    private static final Map<String, Set<String>> SYNONYMS = new HashMap<>();

    static {
        // ============================================================
        // PAIN AND DISCOMFORT - General
        // ============================================================
        addSynonyms("pain",
            "ache", "aching", "hurt", "hurting", "sore", "soreness", "tender", "tenderness",
            "discomfort", "suffering", "agony", "throbbing", "pounding", "stabbing", "sharp pain",
            "dull pain", "burning pain", "shooting pain", "cramping", "cramp", "twinge", "pang",
            "distress", "affliction", "torment", "ouch", "painful", "hurts");

        addSynonyms("acute pain",
            "sudden pain", "sharp pain", "intense pain", "severe pain", "stabbing pain",
            "piercing pain", "excruciating pain", "unbearable pain");

        addSynonyms("chronic pain",
            "persistent pain", "ongoing pain", "long-term pain", "constant pain",
            "continuous pain", "lasting pain", "recurring pain", "lingering pain");

        // ============================================================
        // PAIN BY BODY LOCATION
        // ============================================================

        // Head pain
        addSynonyms("headache",
            "head pain", "head ache", "head hurts", "head pounding", "cephalalgia",
            "splitting headache", "pounding head", "throbbing head", "sore head",
            "cranial pain", "head pressure");

        addSynonyms("migraine",
            "migraine headache", "severe headache", "sick headache", "blinding headache",
            "one-sided headache", "hemicranial pain", "migrainous");

        addSynonyms("tension headache",
            "stress headache", "tight head", "band around head", "pressure headache",
            "muscle tension headache");

        // Facial pain
        addSynonyms("facial pain",
            "face pain", "face ache", "face hurts", "cheek pain", "jaw pain",
            "trigeminal pain", "face throbbing");

        addSynonyms("toothache",
            "tooth pain", "dental pain", "tooth ache", "teeth hurt", "tooth hurts",
            "dental ache", "odontalgia");

        addSynonyms("jaw pain",
            "jaw ache", "mandibular pain", "tmj pain", "jaw hurts", "lockjaw pain",
            "temporomandibular pain");

        // Neck and throat
        addSynonyms("neck pain",
            "neck ache", "sore neck", "stiff neck", "neck hurts", "cervical pain",
            "neck stiffness", "crick in neck", "cervicalgia", "nape pain");

        addSynonyms("sore throat",
            "throat pain", "throat hurts", "scratchy throat", "raw throat",
            "pharyngitis", "throat irritation", "painful swallowing", "odynophagia",
            "burning throat", "strep throat");

        // Chest and heart
        addSynonyms("chest pain",
            "chest ache", "chest hurts", "chest tightness", "chest pressure",
            "thoracic pain", "sternum pain", "breastbone pain", "pectoral pain",
            "angina", "heart pain", "cardiac pain", "precordial pain");

        addSynonyms("heartburn",
            "acid reflux", "indigestion", "pyrosis", "acid indigestion",
            "gastric reflux", "gerd", "gastroesophageal reflux", "burning chest",
            "reflux", "stomach acid", "acid stomach");

        // Back pain
        addSynonyms("back pain",
            "backache", "back ache", "sore back", "back hurts", "spinal pain",
            "dorsalgia", "lumbago", "bad back", "back trouble", "back problems");

        addSynonyms("lower back pain",
            "low back pain", "lumbar pain", "lumbago", "lower backache",
            "sacral pain", "tailbone area pain", "small of back pain");

        addSynonyms("upper back pain",
            "thoracic back pain", "mid back pain", "shoulder blade pain",
            "scapular pain", "interscapular pain");

        // Abdominal pain
        addSynonyms("abdominal pain",
            "stomach pain", "stomach ache", "stomachache", "belly pain", "belly ache",
            "tummy ache", "tummy pain", "gut pain", "intestinal pain", "visceral pain",
            "stomach hurts", "belly hurts", "gastric pain", "epigastric pain",
            "abdominal cramps", "stomach cramps", "colic");

        addSynonyms("cramping",
            "cramps", "spasms", "muscle spasm", "stomach cramps", "abdominal cramps",
            "period cramps", "menstrual cramps", "charley horse", "spasming");

        // Pelvic and reproductive
        addSynonyms("pelvic pain",
            "lower abdominal pain", "groin pain", "hip area pain", "pelvis pain",
            "lower belly pain", "suprapubic pain");

        addSynonyms("menstrual pain",
            "period pain", "period cramps", "menstrual cramps", "dysmenorrhea",
            "painful periods", "cramps during period", "monthly cramps");

        // Limb pain
        addSynonyms("arm pain",
            "arm ache", "arm hurts", "sore arm", "upper extremity pain",
            "brachial pain", "forearm pain", "upper arm pain");

        addSynonyms("leg pain",
            "leg ache", "leg hurts", "sore leg", "lower extremity pain",
            "thigh pain", "calf pain", "shin pain", "leg cramps");

        addSynonyms("joint pain",
            "arthralgia", "achy joints", "sore joints", "joint ache", "joints hurt",
            "articular pain", "joint stiffness", "painful joints", "rheumatic pain");

        addSynonyms("knee pain",
            "knee ache", "sore knee", "knee hurts", "gonalgia", "knee joint pain");

        addSynonyms("shoulder pain",
            "shoulder ache", "sore shoulder", "shoulder hurts", "omalgia",
            "rotator cuff pain", "frozen shoulder");

        addSynonyms("hip pain",
            "hip ache", "sore hip", "hip hurts", "coxalgia", "hip joint pain");

        addSynonyms("elbow pain",
            "elbow ache", "sore elbow", "tennis elbow", "golfers elbow",
            "epicondylitis", "elbow hurts");

        addSynonyms("wrist pain",
            "wrist ache", "sore wrist", "wrist hurts", "carpal pain",
            "carpal tunnel pain");

        addSynonyms("ankle pain",
            "ankle ache", "sore ankle", "ankle hurts", "talalgia");

        addSynonyms("foot pain",
            "feet pain", "foot ache", "sore feet", "foot hurts", "podalgia",
            "plantar pain", "heel pain", "arch pain", "sole pain");

        addSynonyms("hand pain",
            "hand ache", "sore hand", "hand hurts", "palm pain", "finger pain");

        // Muscle pain
        addSynonyms("muscle pain",
            "myalgia", "muscle ache", "muscular pain", "sore muscles", "muscle soreness",
            "muscles hurt", "body aches", "muscle tenderness", "muscle cramps",
            "charley horse", "pulled muscle");

        addSynonyms("fibromyalgia",
            "widespread pain", "chronic muscle pain", "fibro", "body-wide pain",
            "diffuse pain", "all over pain");

        // ============================================================
        // GASTROINTESTINAL SYMPTOMS
        // ============================================================

        addSynonyms("nausea",
            "queasy", "queasiness", "nauseous", "sick to stomach", "feeling sick",
            "stomach upset", "upset stomach", "unsettled stomach", "bilious",
            "about to vomit", "want to throw up", "nauseated", "sick feeling",
            "motion sickness", "car sick", "sea sick", "morning sickness");

        addSynonyms("vomiting",
            "throwing up", "puking", "emesis", "being sick", "getting sick",
            "vomit", "spewing", "retching", "heaving", "regurgitation",
            "barfing", "upchucking", "hurling", "projectile vomiting");

        addSynonyms("diarrhea",
            "diarrhoea", "loose stools", "loose bowels", "runny stools",
            "watery stools", "frequent bowel movements", "the runs", "the trots",
            "upset stomach", "stomach flu", "gastroenteritis", "loose motions",
            "bowel urgency", "explosive diarrhea", "traveler's diarrhea");

        addSynonyms("constipation",
            "constipated", "blocked", "backed up", "difficulty passing stool",
            "hard stools", "infrequent bowel movements", "cant poop", "cant go",
            "straining", "obstipation", "irregularity", "bowel obstruction");

        addSynonyms("bloating",
            "bloated", "abdominal distension", "distended abdomen", "swollen belly",
            "full feeling", "gassy", "gas", "flatulence", "belly swelling",
            "puffed up", "distension", "tympanites", "abdominal swelling");

        addSynonyms("gas",
            "flatulence", "passing gas", "farting", "wind", "intestinal gas",
            "flatus", "belching", "burping", "eructation", "gassy");

        addSynonyms("indigestion",
            "dyspepsia", "upset stomach", "stomach discomfort", "digestive problems",
            "stomach trouble", "poor digestion", "acid stomach", "sour stomach");

        addSynonyms("appetite loss",
            "loss of appetite", "no appetite", "not hungry", "anorexia",
            "decreased appetite", "poor appetite", "dont want to eat",
            "food aversion", "eating less", "reduced appetite");

        addSynonyms("increased appetite",
            "always hungry", "excessive hunger", "polyphagia", "hyperphagia",
            "ravenous", "insatiable appetite", "eating more", "overeating");

        addSynonyms("difficulty swallowing",
            "dysphagia", "trouble swallowing", "cant swallow", "food sticking",
            "swallowing problems", "choking on food", "hard to swallow",
            "globus sensation", "lump in throat");

        addSynonyms("rectal bleeding",
            "blood in stool", "bloody stool", "hematochezia", "bleeding from rectum",
            "blood when wiping", "blood on toilet paper", "melena", "black stool",
            "tarry stool", "bloody poop");

        addSynonyms("hemorrhoids",
            "piles", "rectal pain", "anal pain", "bleeding hemorrhoids",
            "swollen veins", "hemorrhoidal", "anal itching");

        // ============================================================
        // CARDIOVASCULAR SYMPTOMS
        // ============================================================

        addSynonyms("palpitations",
            "heart palpitations", "racing heart", "heart racing", "rapid heartbeat",
            "heart fluttering", "heart flutter", "skipped heartbeat", "irregular heartbeat",
            "heart pounding", "tachycardia", "heart beating fast", "feel my heart beating",
            "heart skipping", "flip flopping heart", "thudding heart");

        addSynonyms("high blood pressure",
            "hypertension", "elevated blood pressure", "raised blood pressure",
            "hbp", "high bp", "blood pressure high");

        addSynonyms("low blood pressure",
            "hypotension", "blood pressure low", "low bp", "dropping blood pressure");

        addSynonyms("edema",
            "swelling", "oedema", "fluid retention", "water retention", "puffy",
            "puffiness", "swollen", "bloated limbs", "swollen ankles", "swollen feet",
            "swollen legs", "swollen hands", "peripheral edema", "pedal edema");

        addSynonyms("shortness of breath",
            "breathlessness", "difficulty breathing", "hard to breathe", "cant breathe",
            "dyspnea", "dyspnoea", "labored breathing", "out of breath", "winded",
            "respiratory distress", "air hunger", "gasping", "breathing difficulty",
            "short of breath", "sob", "breathless", "suffocating");

        addSynonyms("chest tightness",
            "tight chest", "chest constriction", "chest pressure", "heavy chest",
            "chest heaviness", "band around chest", "squeezing chest");

        // ============================================================
        // RESPIRATORY SYMPTOMS
        // ============================================================

        addSynonyms("cough",
            "coughing", "hacking cough", "dry cough", "wet cough", "productive cough",
            "persistent cough", "chronic cough", "barking cough", "whooping cough",
            "tussis", "hacking", "tickly cough", "chesty cough");

        addSynonyms("wheezing",
            "wheeze", "whistling breathing", "noisy breathing", "stridor",
            "breathing sounds", "rattling chest", "chest rattle");

        addSynonyms("congestion",
            "stuffiness", "stuffy nose", "nasal congestion", "blocked nose",
            "clogged nose", "stuffed up", "bunged up", "sinus congestion",
            "chest congestion", "phlegm", "mucus");

        addSynonyms("runny nose",
            "rhinorrhea", "nasal discharge", "nose running", "drippy nose",
            "sniffles", "sniffly", "watery nose", "nasal drip");

        addSynonyms("sneezing",
            "sneeze", "sneezes", "sternutation", "achoo", "nasal irritation");

        addSynonyms("sputum",
            "phlegm", "mucus", "expectoration", "coughing up mucus", "chest mucus",
            "lung mucus", "productive cough", "spitting up phlegm");

        addSynonyms("asthma",
            "asthmatic", "wheezy", "bronchospasm", "airway constriction",
            "reactive airway", "breathing attack", "asthma attack");

        addSynonyms("pneumonia",
            "lung infection", "chest infection", "respiratory infection",
            "pulmonary infection", "bronchopneumonia");

        // ============================================================
        // NEUROLOGICAL SYMPTOMS
        // ============================================================

        addSynonyms("dizziness",
            "dizzy", "lightheaded", "light headed", "lightheadedness", "woozy",
            "wooziness", "giddy", "giddiness", "unsteady", "off balance",
            "room spinning", "head spinning", "vertigo", "vertiginous",
            "faint feeling", "about to faint", "syncope", "presyncope");

        addSynonyms("vertigo",
            "spinning sensation", "room spinning", "world spinning", "rotational dizziness",
            "spinning dizziness", "bppv", "vestibular", "balance problems");

        addSynonyms("fainting",
            "faint", "passed out", "passing out", "syncope", "blackout", "black out",
            "lost consciousness", "losing consciousness", "swooning", "collapse",
            "vasovagal", "near faint", "almost fainted");

        addSynonyms("numbness",
            "numb", "no feeling", "loss of sensation", "tingling", "pins and needles",
            "paresthesia", "paraesthesia", "prickling", "dead feeling",
            "hypesthesia", "sensory loss", "cant feel");

        addSynonyms("tingling",
            "pins and needles", "prickling", "paresthesia", "paraesthesia",
            "numbness", "tingly", "formication", "crawling sensation");

        addSynonyms("tremor",
            "shaking", "trembling", "shaky", "shakiness", "quivering", "quiver",
            "trembling hands", "shaky hands", "uncontrollable shaking",
            "essential tremor", "parkinsonian tremor");

        addSynonyms("seizure",
            "convulsion", "fit", "epileptic seizure", "epilepsy", "seizures",
            "convulsions", "grand mal", "petit mal", "tonic clonic",
            "epileptic fit", "shaking fit");

        addSynonyms("weakness",
            "weak", "muscle weakness", "feeling weak", "loss of strength", "asthenia",
            "fatigue", "feeble", "no energy", "powerless", "debility",
            "generalized weakness", "limb weakness", "paresis");

        addSynonyms("paralysis",
            "paralyzed", "cant move", "loss of movement", "immobility",
            "paralysed", "plegia", "hemiplegia", "paraplegia", "quadriplegia");

        addSynonyms("confusion",
            "confused", "disoriented", "disorientation", "mental confusion",
            "brain fog", "foggy thinking", "unclear thinking", "befuddled",
            "bewildered", "muddled", "cognitive impairment", "altered mental status",
            "delirium", "delirious");

        addSynonyms("memory loss",
            "forgetfulness", "forgetful", "memory problems", "amnesia",
            "cant remember", "memory impairment", "poor memory", "memory issues",
            "short term memory loss", "cognitive decline", "dementia");

        addSynonyms("concentration problems",
            "cant concentrate", "difficulty concentrating", "poor concentration",
            "focus problems", "attention problems", "distracted", "unfocused",
            "brain fog", "mental fog", "cognitive difficulties");

        // ============================================================
        // PSYCHIATRIC / MENTAL HEALTH SYMPTOMS
        // ============================================================

        addSynonyms("anxiety",
            "anxious", "worried", "worrying", "nervousness", "nervous", "worry",
            "apprehension", "apprehensive", "unease", "uneasy", "on edge",
            "panic", "panicky", "panic attack", "fear", "fearful", "dread",
            "anxiousness", "restless", "restlessness", "agitation", "agitated",
            "jittery", "keyed up", "tense", "tension", "stressed", "stress");

        addSynonyms("depression",
            "depressed", "sad", "sadness", "low mood", "feeling down", "down",
            "unhappy", "melancholy", "hopeless", "hopelessness", "despair",
            "despairing", "blue", "feeling blue", "gloom", "gloomy",
            "dysphoria", "dysphoric", "major depression", "clinical depression",
            "depressive", "feeling empty", "empty feeling");

        addSynonyms("insomnia",
            "sleeplessness", "cant sleep", "trouble sleeping", "difficulty sleeping",
            "sleep problems", "sleep disturbance", "waking up at night",
            "early waking", "hard to fall asleep", "staying asleep",
            "poor sleep", "bad sleep", "not sleeping", "sleep difficulties");

        addSynonyms("drowsiness",
            "sleepiness", "sleepy", "drowsy", "somnolence", "somnolent",
            "tired", "fatigued", "lethargy", "lethargic", "sluggish",
            "groggy", "grogginess", "sedation", "sedated");

        addSynonyms("fatigue",
            "tired", "tiredness", "exhaustion", "exhausted", "worn out",
            "weariness", "weary", "lack of energy", "low energy", "no energy",
            "drained", "spent", "run down", "sluggish", "malaise",
            "chronic fatigue", "always tired", "constantly tired");

        addSynonyms("irritability",
            "irritable", "easily annoyed", "short tempered", "cranky", "grumpy",
            "grouchiness", "grouchy", "snappy", "moody", "moodiness",
            "bad mood", "bad temper", "quick to anger", "touchy", "edgy");

        addSynonyms("mood swings",
            "mood changes", "emotional instability", "emotional lability",
            "mood fluctuations", "ups and downs", "bipolar", "manic depressive",
            "emotional changes", "rapid mood changes");

        addSynonyms("hallucinations",
            "seeing things", "hearing things", "visual hallucinations",
            "auditory hallucinations", "hearing voices", "visions",
            "perceptual disturbances", "psychosis", "psychotic");

        addSynonyms("suicidal thoughts",
            "suicidal ideation", "wanting to die", "thoughts of suicide",
            "self-harm thoughts", "suicidal", "death wish", "not wanting to live");

        addSynonyms("nightmares",
            "bad dreams", "night terrors", "scary dreams", "disturbing dreams",
            "vivid dreams", "nightmare disorder");

        // ============================================================
        // DERMATOLOGICAL / SKIN SYMPTOMS
        // ============================================================

        addSynonyms("rash",
            "skin rash", "eruption", "skin eruption", "breakout", "skin breakout",
            "dermatitis", "eczema", "hives", "urticaria", "skin irritation",
            "red patches", "skin spots", "exanthema", "maculopapular rash",
            "skin reaction", "allergic rash");

        addSynonyms("itching",
            "itchy", "itch", "pruritus", "itchiness", "scratching", "skin irritation",
            "prickly skin", "tickling skin", "crawling sensation");

        addSynonyms("hives",
            "urticaria", "wheals", "welts", "skin welts", "raised bumps",
            "itchy bumps", "allergic hives", "nettle rash");

        addSynonyms("dry skin",
            "xerosis", "skin dryness", "flaky skin", "scaly skin", "rough skin",
            "cracked skin", "peeling skin", "skin peeling", "chapped skin");

        addSynonyms("sweating",
            "perspiration", "perspiring", "sweaty", "excessive sweating", "hyperhidrosis",
            "night sweats", "sweating profusely", "breaking out in sweat",
            "cold sweats", "clammy", "diaphoresis");

        addSynonyms("bruising",
            "bruise", "bruises", "contusion", "black and blue", "ecchymosis",
            "easy bruising", "unexplained bruising", "skin discoloration");

        addSynonyms("skin discoloration",
            "discolored skin", "skin color change", "pigmentation", "hyperpigmentation",
            "hypopigmentation", "pale skin", "pallor", "jaundice", "yellowing",
            "cyanosis", "blue skin", "flushing", "redness");

        addSynonyms("acne",
            "pimples", "spots", "zits", "breakouts", "blemishes", "blackheads",
            "whiteheads", "cystic acne", "skin bumps", "facial breakout");

        addSynonyms("hair loss",
            "alopecia", "balding", "bald", "losing hair", "thinning hair",
            "hair falling out", "hair shedding", "receding hairline",
            "pattern baldness", "hair thinning");

        addSynonyms("skin infection",
            "infected skin", "cellulitis", "abscess", "boil", "furuncle",
            "skin ulcer", "wound infection", "impetigo", "skin lesion");

        // ============================================================
        // MUSCULOSKELETAL SYMPTOMS
        // ============================================================

        addSynonyms("stiffness",
            "stiff", "rigidity", "rigid", "tightness", "tight muscles",
            "limited movement", "reduced mobility", "inflexibility",
            "morning stiffness", "joint stiffness", "muscle stiffness");

        addSynonyms("arthritis",
            "joint inflammation", "inflamed joints", "arthritic", "rheumatoid",
            "osteoarthritis", "degenerative joint", "joint disease");

        addSynonyms("swollen joints",
            "joint swelling", "puffy joints", "inflamed joints", "joint inflammation",
            "articular swelling", "synovitis");

        addSynonyms("bone pain",
            "bone ache", "osseous pain", "deep bone pain", "skeletal pain",
            "bones hurt", "aching bones");

        addSynonyms("fracture",
            "broken bone", "bone break", "bone fracture", "crack in bone",
            "stress fracture", "hairline fracture");

        addSynonyms("sprain",
            "twisted", "pulled", "strained", "ligament injury", "torn ligament",
            "ankle sprain", "wrist sprain", "muscle strain");

        // ============================================================
        // UROLOGICAL / RENAL SYMPTOMS
        // ============================================================

        addSynonyms("frequent urination",
            "urinary frequency", "peeing a lot", "going to bathroom a lot",
            "polyuria", "overactive bladder", "frequent peeing", "urinating often",
            "constant need to urinate", "always peeing");

        addSynonyms("painful urination",
            "dysuria", "burning urination", "burning pee", "hurts to pee",
            "pain when urinating", "urination pain", "stinging urination");

        addSynonyms("blood in urine",
            "hematuria", "bloody urine", "red urine", "pink urine",
            "urine with blood", "peeing blood");

        addSynonyms("urinary incontinence",
            "incontinence", "leaking urine", "bladder control problems",
            "cant hold urine", "wetting yourself", "urge incontinence",
            "stress incontinence", "bladder leakage");

        addSynonyms("kidney pain",
            "renal pain", "flank pain", "side pain", "kidney ache",
            "back pain near kidneys", "kidney stones", "nephrolithiasis");

        addSynonyms("urinary tract infection",
            "uti", "bladder infection", "cystitis", "urinary infection",
            "water infection", "urine infection");

        // ============================================================
        // REPRODUCTIVE / SEXUAL SYMPTOMS
        // ============================================================

        addSynonyms("erectile dysfunction",
            "impotence", "ed", "cant get erection", "trouble getting erection",
            "sexual dysfunction", "erection problems", "difficulty getting hard");

        addSynonyms("decreased libido",
            "low sex drive", "reduced sex drive", "loss of libido",
            "no interest in sex", "low libido", "sexual desire decreased",
            "dont want sex", "frigidity");

        addSynonyms("vaginal discharge",
            "discharge", "vaginal secretion", "leukorrhea", "abnormal discharge",
            "white discharge", "yellow discharge");

        addSynonyms("vaginal bleeding",
            "abnormal bleeding", "spotting", "breakthrough bleeding",
            "irregular bleeding", "bleeding between periods", "metrorrhagia");

        addSynonyms("breast pain",
            "mastalgia", "breast tenderness", "sore breasts", "breast ache",
            "tender breasts", "painful breasts", "breast discomfort");

        addSynonyms("hot flashes",
            "hot flushes", "hot flush", "power surge", "night sweats",
            "vasomotor symptoms", "sudden warmth", "flushing", "menopausal symptoms");

        // ============================================================
        // OPHTHALMOLOGICAL / EYE SYMPTOMS
        // ============================================================

        addSynonyms("blurred vision",
            "blurry vision", "vision blurred", "cant see clearly", "fuzzy vision",
            "hazy vision", "unclear vision", "cloudy vision", "visual impairment",
            "vision problems", "difficulty seeing", "poor vision");

        addSynonyms("double vision",
            "diplopia", "seeing double", "two images", "ghost images");

        addSynonyms("eye pain",
            "ocular pain", "eye ache", "painful eyes", "sore eyes", "eyes hurt",
            "eye discomfort", "orbital pain");

        addSynonyms("dry eyes",
            "eye dryness", "gritty eyes", "sandy feeling eyes", "xerophthalmia",
            "burning eyes", "scratchy eyes", "tired eyes");

        addSynonyms("watery eyes",
            "tearing", "eyes watering", "lacrimation", "excessive tears",
            "teary eyes", "epiphora");

        addSynonyms("red eyes",
            "bloodshot eyes", "eye redness", "conjunctival injection",
            "pink eye", "conjunctivitis", "irritated eyes", "inflamed eyes");

        addSynonyms("vision loss",
            "blindness", "blind", "loss of vision", "cant see", "visual loss",
            "vision impairment", "going blind", "losing sight", "amaurosis");

        addSynonyms("light sensitivity",
            "photophobia", "sensitive to light", "light hurts eyes",
            "bright light bothers", "photosensitivity", "glare sensitivity");

        addSynonyms("floaters",
            "eye floaters", "spots in vision", "seeing spots", "vitreous floaters",
            "flying spots", "shadows in vision");

        // ============================================================
        // ENT (EAR, NOSE, THROAT) SYMPTOMS
        // ============================================================

        addSynonyms("ear pain",
            "earache", "ear ache", "otalgia", "ear hurts", "sore ear",
            "ear infection", "otitis", "inner ear pain");

        addSynonyms("hearing loss",
            "deafness", "deaf", "cant hear", "hard of hearing", "hearing impairment",
            "hearing problems", "loss of hearing", "hearing difficulty",
            "muffled hearing", "decreased hearing");

        addSynonyms("tinnitus",
            "ringing in ears", "ear ringing", "ringing ears", "buzzing in ears",
            "humming in ears", "whooshing sound", "ear noise", "ears ringing");

        addSynonyms("nosebleed",
            "nose bleed", "epistaxis", "bloody nose", "bleeding nose",
            "nasal bleeding", "blood from nose");

        addSynonyms("nasal congestion",
            "stuffy nose", "blocked nose", "nose blocked", "congested nose",
            "nasal blockage", "cant breathe through nose", "plugged nose");

        addSynonyms("sinus pain",
            "sinus pressure", "sinusitis", "sinus infection", "sinus headache",
            "facial pressure", "sinus congestion");

        addSynonyms("loss of smell",
            "anosmia", "cant smell", "no sense of smell", "smell loss",
            "reduced smell", "hyposmia");

        addSynonyms("loss of taste",
            "ageusia", "cant taste", "no sense of taste", "taste loss",
            "reduced taste", "hypogeusia", "dysgeusia", "altered taste",
            "metallic taste", "bad taste", "funny taste in mouth");

        addSynonyms("hoarseness",
            "hoarse voice", "raspy voice", "croaky voice", "voice changes",
            "dysphonia", "laryngitis", "lost voice", "voice loss");

        // ============================================================
        // GENERAL / CONSTITUTIONAL SYMPTOMS
        // ============================================================

        addSynonyms("fever",
            "high temperature", "elevated temperature", "pyrexia", "febrile",
            "feverish", "running a fever", "temperature", "hyperthermia",
            "burning up", "feeling hot");

        addSynonyms("chills",
            "shivering", "rigors", "feeling cold", "cold chills", "shaking chills",
            "teeth chattering", "goosebumps", "cold sensation");

        addSynonyms("weight loss",
            "losing weight", "lost weight", "weight reduction", "unintentional weight loss",
            "unexplained weight loss", "cachexia", "wasting", "getting thinner",
            "dropping weight", "slimming");

        addSynonyms("weight gain",
            "gaining weight", "gained weight", "weight increase", "putting on weight",
            "getting heavier", "getting fat", "obesity", "obese", "overweight");

        addSynonyms("malaise",
            "feeling unwell", "not feeling well", "general discomfort", "unwellness",
            "feeling poorly", "feeling off", "under the weather", "not right",
            "generally unwell", "feeling bad", "feeling ill", "sick feeling");

        addSynonyms("dehydration",
            "dehydrated", "lack of fluids", "fluid loss", "dry mouth",
            "excessive thirst", "not enough water", "water loss");

        addSynonyms("thirst",
            "thirsty", "excessive thirst", "polydipsia", "dry mouth",
            "always thirsty", "constant thirst", "increased thirst");

        addSynonyms("loss of consciousness",
            "unconscious", "passed out", "blacked out", "fainted", "coma",
            "unresponsive", "knocked out", "syncope");

        // ============================================================
        // ALLERGIC / IMMUNOLOGICAL SYMPTOMS
        // ============================================================

        addSynonyms("allergic reaction",
            "allergy", "allergies", "allergic response", "hypersensitivity",
            "allergic symptoms", "anaphylaxis", "anaphylactic");

        addSynonyms("anaphylaxis",
            "anaphylactic shock", "severe allergic reaction", "anaphylactic reaction",
            "allergic emergency", "throat closing", "cant breathe from allergy");

        addSynonyms("swollen throat",
            "throat swelling", "laryngeal edema", "angioedema", "throat closing",
            "difficulty breathing", "airway swelling");

        addSynonyms("swollen face",
            "facial swelling", "puffy face", "face puffiness", "facial edema",
            "angioedema", "swollen lips", "lip swelling");

        addSynonyms("sneezing",
            "sneezes", "sternutation", "allergic sneezing", "fits of sneezing");

        // ============================================================
        // INFECTION-RELATED SYMPTOMS
        // ============================================================

        addSynonyms("infection",
            "infected", "infectious", "sepsis", "septic", "bacterial infection",
            "viral infection", "fungal infection", "abscess", "pus");

        addSynonyms("flu",
            "influenza", "flu symptoms", "flu-like symptoms", "flu-like illness",
            "grippe", "seasonal flu", "stomach flu");

        addSynonyms("cold",
            "common cold", "head cold", "chest cold", "cold symptoms",
            "upper respiratory infection", "uri", "viral cold");

        // ============================================================
        // BRITISH VS AMERICAN SPELLING
        // ============================================================

        addSynonyms("diarrhea", "diarrhoea");
        addSynonyms("edema", "oedema");
        addSynonyms("anemia", "anaemia");
        addSynonyms("leukemia", "leukaemia");
        addSynonyms("hemorrhage", "haemorrhage");
        addSynonyms("hemorrhoids", "haemorrhoids");
        addSynonyms("esophagus", "oesophagus");
        addSynonyms("estrogen", "oestrogen");
        addSynonyms("feces", "faeces");
        addSynonyms("pediatric", "paediatric");
        addSynonyms("gynecology", "gynaecology");
        addSynonyms("orthopedic", "orthopaedic");
        addSynonyms("dyspnea", "dyspnoea");
        addSynonyms("apnea", "apnoea");
        addSynonyms("anesthesia", "anaesthesia");
        addSynonyms("tumor", "tumour");
        addSynonyms("color", "colour");
        addSynonyms("behavior", "behaviour");
        addSynonyms("paralyze", "paralyse");
        addSynonyms("analyze", "analyse");
        addSynonyms("realize", "realise");
        addSynonyms("organization", "organisation");

        // ============================================================
        // ADDITIONAL COMMON SEARCHES
        // ============================================================

        addSynonyms("cancer",
            "tumor", "tumour", "malignancy", "malignant", "carcinoma", "neoplasm",
            "oncology", "mass", "growth", "lump", "metastasis", "metastatic");

        addSynonyms("diabetes",
            "diabetic", "blood sugar", "glucose", "hyperglycemia", "hypoglycemia",
            "type 1 diabetes", "type 2 diabetes", "insulin resistance", "sugar diabetes");

        addSynonyms("stroke",
            "cerebrovascular accident", "cva", "brain attack", "mini stroke", "tia",
            "transient ischemic attack", "apoplexy");

        addSynonyms("heart attack",
            "myocardial infarction", "mi", "cardiac arrest", "coronary", "heart failure");

        addSynonyms("bleeding",
            "hemorrhage", "haemorrhage", "blood loss", "bloodloss", "bloody",
            "hemorrhaging", "haemorrhaging");

        addSynonyms("injury",
            "trauma", "wound", "hurt", "damage", "laceration", "cut", "bruise",
            "contusion", "abrasion", "scrape");

        addSynonyms("inflammation",
            "inflamed", "inflammatory", "swelling", "redness", "irritation",
            "itis", "swollen");
    }

    /**
     * Add synonyms to the dictionary. All terms in the list are considered synonyms of each other.
     */
    private static void addSynonyms(String primaryTerm, String... relatedTerms) {
        String lowerPrimary = primaryTerm.toLowerCase().trim();

        // Ensure primary term has an entry
        SYNONYMS.computeIfAbsent(lowerPrimary, k -> new HashSet<>());

        // Add all related terms as synonyms of the primary term
        for (String term : relatedTerms) {
            String lowerTerm = term.toLowerCase().trim();
            SYNONYMS.get(lowerPrimary).add(lowerTerm);

            // Also create reverse mappings so lookups work both ways
            SYNONYMS.computeIfAbsent(lowerTerm, k -> new HashSet<>());
            SYNONYMS.get(lowerTerm).add(lowerPrimary);

            // Add cross-references between all related terms
            for (String otherTerm : relatedTerms) {
                String lowerOther = otherTerm.toLowerCase().trim();
                if (!lowerOther.equals(lowerTerm)) {
                    SYNONYMS.get(lowerTerm).add(lowerOther);
                }
            }
        }
    }

    /**
     * Get all synonyms for a given term.
     * This includes direct synonyms and related terms.
     *
     * @param term The search term
     * @return Set of synonyms (may be empty if no synonyms found)
     */
    public static Set<String> getSynonyms(String term) {
        if (term == null || term.trim().isEmpty()) {
            return Collections.emptySet();
        }

        String lowerTerm = term.toLowerCase().trim();
        Set<String> result = new HashSet<>();

        // Direct lookup
        if (SYNONYMS.containsKey(lowerTerm)) {
            result.addAll(SYNONYMS.get(lowerTerm));
        }

        // Partial match lookup - find synonyms for terms that contain the search term
        for (Map.Entry<String, Set<String>> entry : SYNONYMS.entrySet()) {
            String key = entry.getKey();
            // If the key contains our search term as a word
            if (key.contains(lowerTerm) || lowerTerm.contains(key)) {
                result.addAll(entry.getValue());
                result.add(key);
            }
        }

        // Remove the original term from results
        result.remove(lowerTerm);

        return result;
    }

    /**
     * Get synonyms as a list, limited to a maximum count.
     *
     * @param term The search term
     * @param maxCount Maximum number of synonyms to return
     * @return List of synonyms
     */
    public static List<String> getSynonyms(String term, int maxCount) {
        Set<String> synonyms = getSynonyms(term);
        List<String> result = new ArrayList<>(synonyms);

        // Sort by length (shorter terms first, as they're often more general)
        result.sort(Comparator.comparingInt(String::length));

        if (result.size() > maxCount) {
            return result.subList(0, maxCount);
        }
        return result;
    }

    /**
     * Check if a term has any synonyms in the dictionary.
     *
     * @param term The term to check
     * @return true if synonyms exist
     */
    public static boolean hasSynonyms(String term) {
        if (term == null || term.trim().isEmpty()) {
            return false;
        }
        String lowerTerm = term.toLowerCase().trim();
        return SYNONYMS.containsKey(lowerTerm) && !SYNONYMS.get(lowerTerm).isEmpty();
    }

    /**
     * Get total number of terms in the dictionary.
     */
    public static int getDictionarySize() {
        return SYNONYMS.size();
    }

    /**
     * Expand a search query with synonyms.
     * Returns a list containing the original term plus all its synonyms.
     *
     * @param term The search term
     * @return List containing original term and synonyms
     */
    public static List<String> expandSearchTerms(String term) {
        List<String> expanded = new ArrayList<>();
        if (term == null || term.trim().isEmpty()) {
            return expanded;
        }

        String lowerTerm = term.toLowerCase().trim();
        expanded.add(lowerTerm);
        expanded.addAll(getSynonyms(lowerTerm));

        return expanded;
    }
}
