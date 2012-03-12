<?php
require_once("db.php");
function check_login($username, $pwd){
	$db = connect();
	$res = query($db, "select * from users where username='".$username."' and password='".$pwd."'");
	$row=$res->fetchRow(MDB2_FETCHMODE_ASSOC);
$db->disconnect();
	if($res->numRows() == 1){
			session_start(); 
			$_SESSION["utente"]=$row["username"];
			header('Location: profile.php');
			exit;
	}
	else{
		header('Location: index.php?action=login_error');
	}
}
function check_username_in_use($username){
	$db = connect();
	$res = query($db, "select * from users where username='".$_POST["username"]."'");
	$db->disconnect();
	$row=$res->fetchRow(MDB2_FETCHMODE_ASSOC);
	if(empty($row["username"])){
		return true;
	}
	else{
		header('Location: index.php?action=error&error=inuse');
		exit;
	}
}
function check_session(){
	session_start();
	if (!isset($_SESSION["utente"])){
    		header("Location: index.php");
  		exit;
  	}
}
?>
