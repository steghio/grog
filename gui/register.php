<?php 
require_once("db.php");
require_once("check.php");
require_once("utilities.php");
if(check_username_in_use($_POST["username"])){
	$db = connect();
	query($db, "insert into users values('".$_POST["username"]."','".$_POST["pwd"]."')");
	$db->disconnect();
	header('Location: index.php?action=registered');
}
?>
