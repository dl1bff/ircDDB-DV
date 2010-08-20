


classfiles:
	javac -cp ../ircDDB/ircDDB2.jar net/ircDDB/dv/*.java

classfiles_beta:
	javac -cp ../ircDDB/ircDDB2_beta.jar net/ircDDB/dv/*.java

x: app2.jar

y: app2_beta.jar

app2.jar: classfiles
	./mk_manifest.sh
	jar cmf app.manifest app2.jar  net/ircDDB/dv/*.class
	jarsigner app2.jar dl1bff
 
app2_beta.jar: classfiles_beta
	./mk_manifest.sh beta
	jar cmf app.manifest app2_beta.jar  net/ircDDB/dv/*.class
	jarsigner app2_beta.jar dl1bff

