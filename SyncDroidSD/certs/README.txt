for creating production builds you need to place your certificate into this folder named "release.keystore"

For creating a new keystore use:

keytool -genkey -v -keystore release.keystore -alias syncdroid -keyalg RSA -validity 10000