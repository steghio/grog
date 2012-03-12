<?php
require_once("MDB2.php");
function connect (){
	$db = MDB2::connect("mysql://root:12345@127.0.0.1/gui");
	if (PEAR::isError($db)) {
     		die($db->getMessage());
	}
	return $db;
}
function query($db, $query){
	$res = $db->query($query);
	if (PEAR::isError($res)) {
		die($res->getMessage());
	}
	return $res;
}
?>
