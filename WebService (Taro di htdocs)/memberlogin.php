<?php 
	
	$username=$_POST['username'];
	$password=$_POST['password'];
	
	$connection = mysql_connect("localhost", "root", "");
	
	$username = stripslashes($username);
	$password = stripslashes($password);
	$username = mysql_real_escape_string($username);
	$password = mysql_real_escape_string($password);
	$password = $password;
	
	$db = mysql_select_db("pharos", $connection);
	
	$query = mysql_query("SELECT * FROM staff WHERE password='$password' AND name='$username'", $connection);
	$rows = mysql_num_rows($query);
	if ($rows == 1) {
		$id = mysql_fetch_array($query)["id"];
		
		$checkQuery = mysql_query("SELECT * FROM log WHERE id='$id' AND status=1 AND date>=CURDATE()",$connection);
		$checkRows = mysql_num_rows($checkQuery);
		if($checkRows == 0){ //kalo belum pernah login hari ini
			$insertQuery = "INSERT INTO log(id,status) VALUES('$id','1')";
			$retval = mysql_query($insertQuery, $connection);
			
			if (! $retval){
				echo("fail");
			}
			else echo("success");
		}
		else echo "doublelogin";
		
		
	} 
	else {
		echo "failed";
	}
	mysql_close($connection); // Closing Connection

?>