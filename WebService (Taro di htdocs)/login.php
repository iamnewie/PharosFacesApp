<?php
	session_start();
	$error=''; // Variable To Store Error Message
	if (isset($_POST['submit'])) {
		if (empty($_POST['username']) || empty($_POST['password'])) {
			$error = "Username or Password is invalid";
		}
		else {
			$username=$_POST['username'];
			$password=$_POST['password'];
			
			$connection = mysql_connect("localhost", "root", "");
			
			$username = stripslashes($username);
			$password = stripslashes($password);
			$username = mysql_real_escape_string($username);
			$password = mysql_real_escape_string($password);
			$password = $password;
			
			$db = mysql_select_db("pharos", $connection);
			
			$query = mysql_query("SELECT * FROM login WHERE password='$password' AND username='$username'", $connection);
			$rows = mysql_num_rows($query);
			if ($rows == 1) {
				$_SESSION['login_user']=$username; // Initializing Session
				header("location: attendlist.php"); // Redirecting To Other Page
			} 
			else {
				$error = "Username or Password is invalid";
			}
			mysql_close($connection); // Closing Connection
		}
	}
?>