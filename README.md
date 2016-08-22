# ONIX-Validator
This solution is a Java console program that serves to provide a way for validating files of the ONIX XML format, which is the international standard for representing electronic data regarding books (along with other media).  This format has been established by the international book trade body known as EDITEUR.

Since the ONIX format uses an external DTD or XSD file for validation and since many ONIX files have an HTTP URL to this external reference,
the first step of this program will iterate through a folder and replace the HTTP URL with a local one.  (These DTDs/XSDs should already
be downloaded into this preset folder.)  Then, after this preprocessing, the program will attempt to validate each file, placing 
the valid ones into one folder while moving the invalid ones into another.

# NOTES

In order to download DTDs and XSDs that refer to the legacy versions of ONIX (i.e., 2.1 and below), you should go to EDITEUR's <a target="_blank" href="http://www.editeur.org/15/Archived-Previous-Releases/">download page for previous releases</a>.

In order to download DTDs and XSDs for the latest version of ONIX (i.e., 3.x), you should go to EDITEUR's <a target="_blank" href="http://www.editeur.org/93/Release-3.0-Downloads/">download page for the current release</a>.
