#!/bin/sh


echo "Manifest-Version: 1.0" > app.manifest
echo "Specification-Title: ircDDB DV Plugins" >> app.manifest
echo "Specification-Version: 1.0" >> app.manifest
echo "Specification-Vendor: dl1bff@mdx.de" >> app.manifest
echo "Implementation-Title: ircDDB DV Plugins" >> app.manifest
echo "Implementation-Vendor: dl1bff@mdx.de" >> app.manifest
date '+Implementation-Version: %Y%m%d.%H%M'"$1" >> app.manifest

