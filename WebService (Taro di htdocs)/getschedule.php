<?php
	$username=$_POST['username'];
	
	$connection = mysql_connect("localhost", "root", "");
	
	$username = stripslashes($username);
	$username = mysql_real_escape_string($username);
	
	$db = mysql_select_db("pharos", $connection);
	
	$queryStaff = mysql_query("SELECT * FROM staff WHERE name='$username'", $connection);
	$rows = mysql_num_rows($queryStaff);
	if ($rows == 1) {
		$array = mysql_fetch_array($queryStaff);
		$id = $array["id"];
		$fullname = $array["fullname"];
		$image = $array["image"];
		
		$queryLog = mysql_query("SELECT date FROM log WHERE id = '$id' AND status = '1' AND date >= (DATE_SUB(CURDATE(), INTERVAL 1 MONTH))", $connection); //1 bulan terakhir
		
		echo $id . ";" . $fullname . ";" . $image;
		while($date = mysql_fetch_array($queryLog))
		{
			echo ";".$date['date'];
		}
	} 
	else {
		echo "-;-;";
	}
	mysql_close($connection); // Closing Connection
?>