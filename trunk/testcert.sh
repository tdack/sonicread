#!/bin/sh
#keytool -genkey -keystore srKeystore -alias stacium
#keytool -selfcert -alias stacium -keystore srKeystore
#keytool -list -keystore srKeystore
jarsigner -keystore srKeystore dist/SonicRead.jar stacium
jarsigner -keystore srKeystore dist/lib/swing-worker-1.1.jar stacium
jarsigner -keystore srKeystore dist/lib/beansbinding-1.2.1.jar stacium
jarsigner -keystore srKeystore dist/lib/appframework-1.0.3.jar stacium

