# SSL history

https://www.bearpooh.com/120
'''
openssl genrsa -aes256 -out
openssl genrsa -aes256 -out rootca.key 2048
openssl req -new -key rootca.key -out rootca.csr
openssl x509 -req -days 3650 -set_serial 1 -in rootca.csr -signkey rootca.key -out rootca.crt
openssl genrsa -aes256 -out test.key 2048
openssl rsa -in test.key -out test.np.key
openssl req -new -key test.np.key -out test.csr
openssl x509 -req -days 3650 -extensions v3_user -in test.csr -CA rootca.crt -CAcreateserial -CAkey rootca.key -out test.crt
openssl x509 -text -in test.crt
openssl x509 -in test.crt -out test.pem -outform PEM
openssl pkcs12 -export -name test -in test.pem -inkey test.key -out test.pfx
keytool -importkeystore -srckeystore test.pfx -srcstoretype pkcs12 -destkeystore test.jks -deststoretype jks

'''