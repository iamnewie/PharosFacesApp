<?php
	include('session.php'); //ambil db + store session
?>
<html>
<head>
	<title>PharosFaces</title>
	<link href="style.css" rel="stylesheet" type="text/css">
</head>
<body>
	<div id="profile">
	<b id="welcome">Welcome : <i><?php echo $login_session; ?></i></b>
	<h1>Today Attendance List ( <?php echo date("d/m/Y"); ?> )</h1>
	<form action='<?php echo $_SERVER['PHP_SELF']; ?>' method='post' name='form_filter'>
		<div class="styled-select">
			<select name="selectDiv">
				<option value="" disabled selected>Choose division</option>
				<option value="all">All</option>
				<?php
					$sql=mysql_query("SELECT DISTINCT division FROM staff");
					if(mysql_num_rows($sql)){
						while($rs=mysql_fetch_array($sql)){
							echo "<option value=" .$rs['division']. ">" .$rs['division']. "</option>";
						}
					}
				?>
			</select>
		</div>
		<input type='submit' value = 'Filter'>
	</form>
	<table class="responstable">
	<tr>
	<th>Staff ID</th>
	<th>Staff Name</th>
	<th>Division</th>
	<th>Login Time</th>
	<th>Logout Time</th>
	</tr>
	<?php		
		if(empty($_POST['selectDiv'])) $_POST['selectDiv']="all";
		//$result = mysql_query("SELECT * FROM log WHERE date >= CURDATE()");
		if($_POST['selectDiv'] != "all") $sql = "SELECT * FROM staff WHERE division LIKE '".$_POST['selectDiv']."'";
		else $sql = "SELECT * FROM staff";
		$result = mysql_query($sql) or trigger_error(mysql_error().$sql);
		$timeLimit = "09:00:00";
		
		while($row = mysql_fetch_array($result)){
			echo "<tr>";
			$id = $row["id"];
			$name = $row["name"];
			$division = $row["division"];
			
			//ambil login time
			$loginTime = mysql_fetch_array(mysql_query("SELECT DATE_FORMAT(date, '%H:%i:%s') as time FROM log WHERE date >= CURDATE() AND status = 1 AND id = ".$id))["time"];
			if($loginTime == "") $loginTime = "-";
			
			//ambil logout time
			$logoutTime = mysql_fetch_array(mysql_query("SELECT DATE_FORMAT(date, '%H:%i:%s') as time FROM log WHERE date >= CURDATE() AND status = 0 AND id = ".$id))["time"];
			if($logoutTime == "") $logoutTime = "-";
			
			echo "<td>$id</td>";
			echo "<td>$name</td>";
			echo "<td>$division</td>";
			echo "<td>";
			if($loginTime > $timeLimit) echo "<font color='red'>$loginTime</font>"; //kalo login lewat jam limit
			else echo $loginTime;
			echo "</td>";
			
			echo "<td>";
			echo $logoutTime;
			echo "</td>";
			echo "</tr>";
		}
	?>
	</table>
	<b class="btn orange float-right"><a href="logout.php">Logout</a></b>
	</div>
</body>
</html>