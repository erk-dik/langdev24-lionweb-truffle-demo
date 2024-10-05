mvn package clean
mvn -DskipTests=true -Pnative package
./target/fib.exe