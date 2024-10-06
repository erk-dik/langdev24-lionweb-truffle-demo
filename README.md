## langdev24-lionweb-truffle-demo

This repo includes the demo code presented in langdev conference 2024:

A Case Study: Execution of LionWeb nodes in Truffle Language Framework (https://langdevcon.org/2024/program#31)

### Setup:

1. Download Oracle GraalVM: https://www.graalvm.org/downloads/
2. Set the JAVA_HOME and PATH to GraalVM SDK
3. Clone this repository
4. Compile the application:

   `mvn -DskipTests=true -Pnative package`

5. Execute:

   `./target/fib.exe`

