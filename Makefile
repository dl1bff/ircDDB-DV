


classfiles:
	javac -cp ../ircDDB/ircDDB.jar net/ircddb/dv/*.java

classfiles_beta:
	javac -cp ../ircDDB/ircDDB_beta.jar net/ircddb/dv/*.java

x: app_beta.jar

app.jar: classfiles
	./mk_manifest.sh
	jar cmf app.manifest app.jar  net/ircddb/dv/*.class
	jarsigner app.jar dl1bff
 
app_beta.jar: classfiles_beta
	./mk_manifest.sh beta
	jar cmf app.manifest app_beta.jar  net/ircddb/dv/*.class
	jarsigner app_beta.jar dl1bff

