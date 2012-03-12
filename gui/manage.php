<?php
require_once("check.php");
check_session();
require_once("utilities.php");
$id = substr($_POST["appid"], 0, strlen($_POST["appid"])-4);

$titolo = "Grog GUI - ".$_SESSION["utente"]." - ".$id;
echo "<head><title>".$titolo."</title></head>";
echo <<<PRINT
<form action ="start.php" method="post">
<input type="hidden" name="appid" value="$id"/>
<input type="submit" name="start" value="Start"/>
</form>
<form action ="stop.php" method="post">
<input type="hidden" name="appid" value="$id"/>
<input type="submit" name="stop" value="Stop"/>
</form>
<form action ="scale.php" method="post">
<input type="hidden" name="appid" value="$id"/>
Instances: <input type="text" name="instances" value=""/>
<input type="submit" name="scale" value="Scale"/>
</form>

PRINT;

include("./include/logout.tmp");
?>
