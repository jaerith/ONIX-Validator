# ONIX-Validator
This solution is a Java console program that serves to provide a way for validating files of the ONIX XML format, which is the international standard for representing electronic data regarding books (along with other media).

Since the ONIX format uses an external DTD or XSD file for validation and since many ONIX files have an HTTP URL to this external reference,
the first step of this program will iterate through a folder and replace the HTTP URL with a local one.  (These DTDs/XSDs should already
be downloaded into this preset folder.)  Then, after this preprocessing, the program will attempt to validate each file, placing 
the valid ones into one folder while moving the invalid ones into another.
