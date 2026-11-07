# READABILITY-BG

Evaluation of text readability for Bulgarian readers – native speakers by school grade (L1) and learners of Bulgarian by CEFR level (L2). The tool compares a text with the expected knowledge and skills of a target reader, gives an overall assessment and points out the parts of the text that may be difficult, with explanations and suggestions.

**[English](#english) | [Български](#български)**

---

## English

### What it does

- **Readability score** – FRE_bg = 206.835 − 1.3 · ASL − 52.5 · ASW on a 100–0 scale. The score is mapped to school grades by linear interpolation: 100 = grade 1 and 60 = grade 12, with grades 10 and 12 treated as one group.
- **Indices calibrated for Bulgarian** – LIX_bg = ASL + LW≥8; CLI_bg = 0.0594·L − 0.4035·S − 20.33; Fog_bg = 0.4·(ASL + PW≥5); SMOG_bg = 1.0430·√(NPW≥5·30/sentences) + 3.1291; Pisarek_bg = 0.94·ASL/3 + 0.73·PW≥4/3 + 1. CLI_bg, Fog_bg and SMOG_bg give a school grade and Pisarek_bg years of schooling; LIX_bg has no grade scale and is compared with the reference texts.
- **Lexical coverage** against a lexical minimum:
  - **direct** – the lemma is in the list;
  - **extended** – the lemma or a related word with the same root is in the list (e.g. *бавно* ← *бавен*, *катеричка* ← *катерица*, *изям* ← *ям*).
- **Text features at all language levels**:
  - phonological and graphic: sentence and word length, words with 8+ letters, words with 5+ syllables;
  - lexical: lexical density, coverage, share of abstract nouns;
  - morphological: analytic verb forms per sentence; ambiguous, passive and evidential verb forms;
  - syntactic: clauses, subordinate clauses by type, coordination, tree depth, dependency distance;
  - text: pronominal anaphora, Pro-drop, distance to the antecedent.
- **Comparison with the reader**:
  - Each feature is compared with reference values computed on the national exam texts (НВО grades 4, 7, 10; ДЗИ grade 12) and marked as *easy*, *manageable* or *difficult* for the reader.
  - L2 levels are mapped to grade equivalents.
  - The rules are stricter for grades 1–3, for level A1 and, for syntactic and text features, for deaf readers.
- **Explanations** (in Bulgarian) of what may be difficult and how to help, plus the places in the text: long sentences, words outside the minimum, complex verb forms, distant antecedents.

### Repository contents

```
README.md
index.html                   web application (Bulgarian)
ReadabilityEvaluator.jar     command-line tool (includes the annotation tools)
build.sh                     builds the jar from src/
src/                         Java source: readability + VerbFormTagger, ClauseSplitter,
                             NounPhraseRecogniser, AnaphoraResolver
resources/
  features.tsv               33 text features and indices: level, name, direction, unit, definition
  reference_values.tsv       mean and quartiles of each feature for grades 4, 7 and 10 (10+12)
  levels.tsv                 FRE_bg ↔ grade anchors, CEFR ↔ grade, grade ↔ reference group
  explanations_bg.tsv        problem : explanation : suggestion (in Bulgarian)
  derivation.tsv             prefixes and suffixes for the extended coverage
  lexicons/
    lexicon_L1_grades1-4.tsv 10,135 basic lexical units for grades 1–4 (L1)
    lexicon_L2_A1-A2.tsv     1,519 units of the A1–A2 lexical minimum (L2)
    abstract_nouns_bg.tsv    2,014 predominantly abstract nouns from BulTreeBank WordNet
tool-resources/              rules and resources of the four annotation tools
profiles/                    example reader profiles
demo-texts/                  15 exam texts (5 per grade group: 4, 7, 10+12), CoNLL-U Plus with
                             all annotations, their features (.json) and index.json
```

Both lexicons use the same format: `lemma`, `upos`, `reflexive` (се/си/-), `level`, `english`.

The abstract nouns are taken from BulTreeBank WordNet (BTB-WN, Simov & Osenova 2010, CC BY 3.0, via Open Multilingual Wordnet 1.4). A sense is abstract if it is under *abstraction.n.06* and not under *physical_entity.n.01* in Princeton WordNet 3.0. A noun is in the list if at least half of its senses are abstract. The list can be replaced with BulNet data in the same format.

### Command-line tool

Requires Java 8 or later.

```bash
java -jar ReadabilityEvaluator.jar -i text.conllu -p profiles/grade4_L1.txt
java -jar ReadabilityEvaluator.jar -i text.conllu -p profile.txt -json result.json -o text_annotated.conllup
java -jar ReadabilityEvaluator.jar -i text.conllu -json text.json      # features only, for index.html
```

- **Input** is a text annotated with [UDPipe 2](https://lindat.mff.cuni.cz/services/udpipe/) (`bulgarian-btb-ud-2.17-251125`). If the DCL annotation columns are missing, the jar adds them itself with VerbFormTagger, ClauseSplitter, NounPhraseRecogniser and AnaphoraResolver.
- **Output** is a report in Bulgarian. Optionally it also writes JSON (`-json`) and the fully annotated text (`-o`).
- **`-resources <folder>`** replaces any built-in resource with a file of the same name, e.g. another lexicon or edited explanations.
- **Rebuilding the reference values** from annotated texts named `gradeNN_*.conllup`:

```bash
java -jar ReadabilityEvaluator.jar -reference annotated/ -metadata metadata.tsv -out resources/reference_values.tsv
```

### Reader profile

A plain text file with `key: value` lines:

| Key | Values |
|---|---|
| `language` | `L1` (Bulgarian as first language) or `L2` (second language) |
| `grade` | school grade 1–12 (L1); if missing, it is computed from `age` |
| `age` | age in years |
| `cefr` | `A1`–`C2` (L2) |
| `needs` | `none` or `deaf` |
| `lexicon` | `L1`, `L2` or a path to a custom list in the same format |
| `known` | comma-separated features the reader already knows (from `features.tsv`); they are not marked as difficult |

### Web application

`index.html` lets the user:
1. load one of the demo texts (or a JSON file produced by the jar);
2. describe the reader in a form;
3. get the evaluation: the overall assessment, a scale showing where the text and the reader are, cards with the key features by language level and with the calibrated indices (marked *по силите*, *с подкрепа* or *разяснение*), the full feature profile against the reader's grade, explanations, and the text with the problem places highlighted.

The evaluation in the browser follows the same steps as the jar. Open it through a web server, e.g. GitHub Pages or `python3 -m http.server` in the repository folder.

### Authors

Department of Computational Linguistics, Institute for Bulgarian Language "Prof. Lyubomir Andreychin", Bulgarian Academy of Sciences

- Ivelina Stoyanova – iva@dcl.bas.bg
- Svetlozara Leseva – zarka@dcl.bas.bg

The annotation tools are also available separately: [SYNTACTIC-BG](https://github.com/DCL-IBL/SYNTACTIC-BG).

---

## Български

### Какво прави

- **Оценка на четивността** – FRE_bg = 206,835 − 1,3 · ASL − 52,5 · ASW по скала 100–0. Стойността се съотнася с клас чрез линейна интерполация: 100 = 1. клас, 60 = 12. клас (10. и 12. клас са една група).
- **Индекси, калибрирани за български** – LIX_bg = ASL + LW≥8; CLI_bg = 0,0594·L − 0,4035·S − 20,33; Fog_bg = 0,4·(ASL + PW≥5); SMOG_bg = 1,0430·√(NPW≥5·30/изречения) + 3,1291; Pisarek_bg = 0,94·ASL/3 + 0,73·PW≥4/3 + 1. CLI_bg, Fog_bg и SMOG_bg дават клас, а Pisarek_bg – години обучение; LIX_bg няма скала по класове и се сравнява с референтните текстове.
- **Лексикално покритие** спрямо лексикален минимум:
  - **пряко** – лемата е в списъка;
  - **разширено** – лемата или сродна дума със същия корен е в списъка (напр. *бавно* ← *бавен*, *катеричка* ← *катерица*, *изям* ← *ям*).
- **Характеристики на текста на всички езикови равнища**:
  - фонологични и графични: дължина на изреченията и думите, думи с 8 или повече букви, думи с 5 или повече срички;
  - лексикални: лексикална плътност, покритие, дял на абстрактните съществителни;
  - морфологични: аналитични глаголни форми в изречение; многозначни, страдателни, преизказни и умозаключителни форми;
  - синтактични: прости изречения, подчинени изречения по видове, съчинение, дълбочина на дървото, разстояние между зависимите думи;
  - текстови: местоименни анафори, изпуснат подлог, разстояние до антецедента.
- **Съпоставка с читателя**:
  - Всяка характеристика се сравнява с референтните стойности, изчислени върху изпитните текстове (НВО в 4., 7. и 10. клас; ДЗИ в 12. клас), и се оценява като *лесно*, *приемливо* или *трудно* за читателя.
  - Нивата по ОЕЕР се съотнасят с клас.
  - Оценката е по-строга за 1.–3. клас, за ниво A1 и – при синтактичните и текстовите характеристики – за глухи читатели.
- **Обяснения** какво може да затрудни читателя и как да се помогне, както и местата в текста: дълги изречения, думи извън минимума, сложни глаголни форми, отдалечени антецеденти.

### Съдържание на хранилището

```
README.md
index.html                   уебприложение (на български)
ReadabilityEvaluator.jar     програма за командния ред (включва инструментите за анотация)
build.sh                     компилиране на jar файла от src/
src/                         изходен код на Java: оценка на четивността + VerbFormTagger, ClauseSplitter,
                             NounPhraseRecogniser, AnaphoraResolver
resources/
  features.tsv               33 характеристики и индекси: равнище, име, посока, единица, определение
  reference_values.tsv       средна стойност и квартили на всяка характеристика за 4., 7. и 10. клас (10+12)
  levels.tsv                 FRE_bg ↔ клас, ниво по ОЕЕР ↔ клас, клас ↔ референтна група
  explanations_bg.tsv        проблем : обяснение : предложение
  derivation.tsv             представки и наставки за разширеното покритие
  lexicons/
    lexicon_L1_grades1-4.tsv 10 135 основни лексикални единици за 1.–4. клас (първи език)
    lexicon_L2_A1-A2.tsv     1519 единици от лексикалния минимум А1–А2 (втори език)
    abstract_nouns_bg.tsv    2014 предимно абстрактни съществителни от BulTreeBank WordNet
tool-resources/              правилата и ресурсите на четирите инструмента за анотация
profiles/                    примерни профили на читатели
demo-texts/                  15 изпитни текста (по 5 за 4., 7. и 10.+12. клас) в CoNLL-U Plus с цялата
                             анотация, характеристиките им (.json) и index.json
```

Двата лексикални списъка са в еднакъв формат: `lemma`, `upos`, `reflexive` (се/си/-), `level`, `english`.

Абстрактните съществителни са извлечени от BulTreeBank WordNet (BTB-WN, Simov & Osenova 2010, CC BY 3.0, чрез Open Multilingual Wordnet 1.4). Едно значение е абстрактно, ако в Princeton WordNet 3.0 е под *abstraction.n.06* и не е под *physical_entity.n.01*. Съществителното е в списъка, ако поне половината от значенията му са абстрактни. Списъкът може да се замени с данни от BulNet в същия формат.

### Програма за командния ред

Изисква Java 8 или по-нова версия.

```bash
java -jar ReadabilityEvaluator.jar -i text.conllu -p profiles/grade4_L1.txt
java -jar ReadabilityEvaluator.jar -i text.conllu -p profile.txt -json result.json -o text_annotated.conllup
java -jar ReadabilityEvaluator.jar -i text.conllu -json text.json      # само характеристиките, за index.html
```

- **Входът** е текст, анотиран с [UDPipe 2](https://lindat.mff.cuni.cz/services/udpipe/) (`bulgarian-btb-ud-2.17-251125`). Ако липсват колоните на DCL, програмата ги добавя сама с VerbFormTagger, ClauseSplitter, NounPhraseRecogniser и AnaphoraResolver.
- **Резултатът** е отчет на български. По желание се записват и JSON (`-json`) и текстът с цялата анотация (`-o`).
- **`-resources <папка>`** замества всеки вграден ресурс с файл със същото име, напр. друг лексикален списък или редактирани обяснения.
- **Референтните стойности** се изчисляват наново от анотирани текстове с имена `gradeNN_*.conllup`:

```bash
java -jar ReadabilityEvaluator.jar -reference annotated/ -metadata metadata.tsv -out resources/reference_values.tsv
```

### Профил на читателя

Текстов файл с редове `ключ: стойност`:

| Ключ | Стойности |
|---|---|
| `language` | `L1` (български като първи език) или `L2` (като втори) |
| `grade` | клас 1–12 (L1); ако липсва, се изчислява от `age` |
| `age` | възраст в години |
| `cefr` | `A1`–`C2` (L2) |
| `needs` | `none` или `deaf` (глух читател) |
| `lexicon` | `L1`, `L2` или път до собствен списък в същия формат |
| `known` | познати на читателя явления (ключове от `features.tsv`), разделени със запетая; те не се отбелязват като трудни |

### Уебприложение

В `index.html` потребителят:
1. зарежда демонстрационен текст (или JSON файл, получен с програмата);
2. описва читателя във формуляр;
3. получава оценката: обща оценка, скала, която показва къде е текстът и къде е читателят, карти с основните характеристики по езикови равнища и с калибрираните индекси (с оценка *по силите*, *с подкрепа* или *разяснение*), пълния профил на текста спрямо класа на читателя, обяснения и текста с отбелязаните трудни места.

Оценката в браузъра следва същите стъпки като програмата. Приложението се отваря през уеб сървър, напр. GitHub Pages или `python3 -m http.server` в папката на хранилището.

### Автори

Секция по компютърна лингвистика, Институт за български език „Проф. Любомир Андрейчин“, Българска академия на науките

- Ивелина Стоянова – iva@dcl.bas.bg
- Светлозара Лесева – zarka@dcl.bas.bg

Инструментите за анотация са достъпни и отделно: [SYNTACTIC-BG](https://github.com/DCL-IBL/SYNTACTIC-BG).
