<?php
require_once("check.php");
check_session();
require_once("utilities.php");
$titolo = "Grog GUI - ".$_SESSION["utente"]." - Home";
echo "<head><title>".$titolo."</title></head>";
$ret = show_profile($_SESSION["utente"]);
echo $ret;
include("./include/upload.tmp");
include("./include/logout.tmp");
?>
