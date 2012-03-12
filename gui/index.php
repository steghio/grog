<?php
session_start();
	if (isset($_SESSION["utente"])){
    		header("Location: profile.php");
  		exit;
  	}
require_once("utilities.php");
global $titolo;
		/*se ci sono arrivato diretto*/
		if(empty($_GET["action"])){
			$titolo="Grog - GUI";
			echo "<head><title>".$titolo."</title></head><h2>Welcome, please login:</h2>";
			include("./include/login.tmp");
		}
		else{
			/*se invece ho navigato per il sito*/
			switch($_GET["action"]){
				/*ho provato a loggarmi ma non sono registrato*/
				case "login_error":{
					$titolo = "Grog - Register";
					
					echo "<h2>Wrong username or password:</h2>";
					include("./include/login.tmp");
					
					break;
				}
				/*mi voglio registrare*/
				case "register":{
					$titolo = "Grog - Register";
					
					echo "<h2>Please fill all fields:</h2>";
					include("./include/register.tmp");
					
					break;
				}
				/*mi sono appena registrato*/
				case "registered":{
					$titolo="Grog - Registration successful";
					
					echo"<h2>Thank you for registering, please login:</h2>";
					include("./include/login.tmp");
					
					break;
				}
				/*ha fatto logout*/
				case "logout":{
					$titolo="Grog - Logout";
					
					echo "<h2>Thank you for using Grog, come back soon!</h2>";
					include("./include/login.tmp");
					
					break;
				}
				/*ha fatto casino l'utente*/
				case "error":{
					switch($_GET["error"]){
						/*non ha messo username*/
						case "nouser":{
							$titolo="Grog - Error";
							
							echo "<h2>You must insert a username:</h2>";
							include("./include/register.tmp");
							
							break;
						}
						/*non ha messo password*/
						case "nopwd":{
							$titolo="Grog - Error";
							
							echo "<h2>You must insert a password:</h2>";
							include("./include/register.tmp");
							
							break;
						}	
						/*ha ciccato il controllo password*/
						case "pwdcheck":{
							$titolo="Grog - Error";
							
							echo "<h2>Passwords do not match:</h2>";
							include("./include/register.tmp");
							
							break;
						}
						/*nome utente già in uso*/
						case "inuse":{
							$titolo="Grog - Error";
							
							echo "<h2>Username select is already in use:</h2>";
							include("./include/register.tmp");
							
							break;
						}
						/*non ha inserito pwd per login*/
						case "nopwdlogin":{
							$titolo="Grog - Error";
							
							echo "<h2>You must enter a password:</h2>";
							include("./include/login.tmp");
							
							break;
						}
						/*cerca di fare il furbo col get*/
						default:{
							echo "Do not touch my URLs!";
						}
					}
				break;
				}
				/*cerca di fare il furbo col get*/
				default:{
							echo "Do not touch my URLs!";
				}
			}
		}
?>
