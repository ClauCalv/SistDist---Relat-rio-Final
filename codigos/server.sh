#!/bin/sh
javac -cp compute.jar rmi/engine/ComputeEngine.java
java -cp .:compute.jar -Djava.rmi.server.codebase=file:. -Djava.rmi.server.hostname=localhost -Djava.security.policy=rmi/engine/server.policy rmi.engine.ComputeEngine

