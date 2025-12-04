# TaskPlanner
A lightweight Java desktop application to take care of tasks.
It uses Maven, SQLite and Swing.
### Requierements
Ensure that you have installed the follwing beforehand:
- Java
  - Check version with:  "java --version"
- Maven
  - Check version with: "mvn -v"
### Downloading the Project
- Option 1: Direct donwload from GitHub
  - Click Code -> Download ZIP
  - Extract to path you want it to be save
- Option 2: Clone repository
  - git clone https://github.com/domsel19/taskplanner.git
  - cd taskplanner
### Building the application
- navigate to project folder in cmd/bash where you can find the pom.xml
- use "mvn clean package" to download dependencies, compile code and create runnable JAR-file
### Running the application
- use "java -jar target/TaskPlanner-1.0-SNAPSHOT.jar" to open
### Notes concerning SQL-file
- SQL-database tasks.db will be create in the folder
