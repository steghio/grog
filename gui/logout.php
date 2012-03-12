<?php
require_once("check.php");check_session();
session_destroy();
	header('Location: index.php?action=logout');
?>
