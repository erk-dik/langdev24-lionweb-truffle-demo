# Playbook: Langdev 2024 Lionweb-Truffle Demo

## 0. Preparation

* Mirror the screen
* Zoom in: set font size to higher number

## 1. MPS Demo

* Open MPS 2021.1.4
    * Show SimpleLanguage language structure
    * Show M1 model (M1)
* Run exporters:
    * Open Messages window
    * Alt + Enter: to run export MPS language to Json
    * Alt + Enter: to run export instance to Json
    * Show output file in Messages window

## 2. Java/Truffle Demo

* Open IntelliJ IDEA
    * start native build:

      `mvn -DskipTests=true -Pnative package`

* Go to resources to show fib.json, simpleLanguage.json.
    * Show node value "10" in fib.json
* Show fib.sl
* Go to demo -> App.java
    * Show input variables
    * Show methods: deserialize, convertToTruffleNodes, executeTruffleNodes methods
* Go to terminal:
    * Execute the binary:

      `./target/fib.exe`

    * Run app on jvm:

      `source run_on_jvm.sh`

* Go to fib.sl and fib.json, update num to 100.
* Rebuild and re-execute.

Notes:

MPS in zoom mode:

    1.1 Setting -> Appearance -> Use custom font
    1.2 Editor -> Font