<?php
require_once("check.php");
check_session();
require_once("utilities.php");

/*
java -jar SendScaleAppMessage
*/

header('Location: manage.php');

?>
