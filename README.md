# Simple File Manager

Simple File Manager was created to address a practical need: deploying a lightweight file-management tool inside a Tomcat or WebLogic server. It provides a straightforward way to browse, access, and manage files belonging to an application running on the same application server, especially in environments where no other file-handling interfaces are available.

Typical use cases include:

- Editing configuration files (e.g., application.properties, logback.xml, etc.)
- Reviewing or downloading log files
- Moving or renaming files and directories
- Quickly inspecting generated artifacts or temporary files

## Build && Run

Build the docker image with the latest code and run it by issuing the following command:
```shell
docker image build -t simple-file-manager . && docker container run -p 9000:8080 --rm --name sfm -v .:/tmp/marce simple-file-manager
```

Then fire browser up and head to http://host:9000/sfm

## Get the sfm.war file

Make sure you have built the Docker image containing the war file you want to get. Then run the following command:

```shell
docker container run -p 9000:8080 --rm --name sfm -v ./out:/out simple-file-manager cp /usr/local/tomcat/webapps/sfm.war /out
```

That will leave the sfm.war file inside the ./out directory, in this folder.
