<?php
require_once("check.php");
require_once("utilities.php");
check_session();

// make a note of the current working directory, relative to root.
$directory_self = str_replace(basename($_SERVER['PHP_SELF']), '', $_SERVER['PHP_SELF']);

// make a note of the directory that will recieve the uploaded file
$uploadsDirectory = './tmp/';

// make a note of the location of the upload form in case we need it
$uploadForm = 'http://' . $_SERVER['HTTP_HOST'] . $directory_self . 'profile.php';

// fieldname used within the file <input> of the HTML form
$fieldname = 'file';

// Now let's deal with the upload

// possible PHP upload errors
$errors = array(1 => 'Superata la dimensione massima del file',
                2 => 'Superata la dimensione massima del file',
                3 => 'Upload non completato',
                4 => 'Nessun file selezionato');

// check the upload form was actually submitted else print the form
//isset($_POST['submit'])
//    or error('SUBMIT', $uploadForm);

// check for PHP's built-in uploading errors
($_FILES[$fieldname]['error'] == 0)
    or error($errors[$_FILES[$fieldname]['error']], $uploadForm);
    
// check that the file we are working on really was the subject of an HTTP upload
@is_uploaded_file($_FILES[$fieldname]['tmp_name'])
    or error('HTTP', $uploadForm);
    
$uploadFilename = $_FILES[$fieldname]['name'];

// now let's move the file to its final location and allocate the new filename to it
@move_uploaded_file($_FILES[$fieldname]['tmp_name'], $uploadsDirectory.$uploadFilename)
    or error('La cartella di salvataggio non ha permessi sufficienti a ricevere il file', $uploadForm);


$filetoup = $uploadsDirectory.$uploadFilename;

exec("curl --user openfiler:password -T {$filetoup} http://192.168.23.94/mnt/storage/nas1/apps/");

unzip($uploadsDirectory.$uploadFilename);

/*
USA CURL PER METTERLO SU WEBDAV
GET: wget --http-user="openfiler" --http-password="password" http://192.168.23.94/mnt/storage/nas1/apps/NOMEFILE
PUT: curl --user openfiler:password -T NOMEFIL http://192.168.23.94/mnt/storage/nas1/apps/
*/

require_once("db.php");
$db =connect();
query($db, "insert into apps values ('".$_FILES[$fieldname]['name']."','".$_SESSION["utente"]."');");
$db->disconnect();
    
// If you got this far, everything has worked and the file has been successfully saved.
// We are now going to redirect the client to a success page.
header('Location: profile.php');

// The following function is an error handler which is used
// to output an HTML error page if the file upload fails
function error($error, $location, $seconds = 3)
{
    header("Refresh: $seconds; URL=\"$location\"");
    echo '<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"'."\n".
    '"http://www.w3.org/TR/html4/strict.dtd">'."\n\n".
    '<html lang="it">'."\n".
    '    <head>'."\n".
    '        <meta http-equiv="content-type" content="text/html; charset=iso-8859-1">'."\n\n".
    '    <title>Errore durante l\'upload</title>'."\n\n".
    '    </head>'."\n\n".
    '    <body>'."\n\n".
    '    <div id="Upload">'."\n\n".
    '        <h1>E\' avvenuto un errore durante il caricamento</h1>'."\n\n".
    '        <p>Descrizione dell\'errore: '."\n\n".
    '        <span class="red">' . $error . '...</span>'."\n\n".
    '         Sto ricaricando la pagina</p>'."\n\n".
    '     </div>'."\n\n".
    '</html>';
    exit;
} // end error handler

?> 
