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
docker image build -t simple-file-manager . && \
docker container run \
  -p 9000:8080 \
  --rm \
  --name sfm \
  -v .:/tmp/myrepo \
  simple-file-manager

```

Then fire browser up and head to http://host:9000/sfm

## Get the sfm.war file

Make sure you have built the Docker image containing the war file you want to get. Then run the following command:

```shell
docker container run \
    -p 9000:8080 \
    --rm \
    --name sfm \
    -v ./out:/out \
    simple-file-manager \
    cp /usr/local/tomcat/webapps/sfm.war /out
```

That will leave the sfm.war file inside the ./out directory, in this folder.

## Disclaimer

Simple File Manager was developed by a monkey with a shotgun (me). I am not a developer, and this webapp was built with the help of an AI agent. As a result, nothing in this software should be assumed to follow proper engineering practices, security standards, or production-grade quality.

I take no responsibility for any bad practices, security breaches, vulnerabilities, data loss, unauthorized access, service interruptions, or any other direct or indirect damage resulting from the use, installation, or modification of this tool. Users are solely responsible for evaluating the risks, securing their environment, and determining whether this application is suitable for their needs.

Installation and usage of Simple File Manager are entirely at the user’s own risk. No official support is provided, no updates are guaranteed, and there is no commitment to maintain this project in the future.
