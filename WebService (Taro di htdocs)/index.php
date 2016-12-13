<?php	
	include('login.php');
	
	if(isset($_SESSION['login_user'])){
		header("location: attendlist.php");
	}
?>
<!DOCTYPE html>
<html>
<head>
	<title>PharosFaces</title>
	<link href="style.css" rel="stylesheet" type="text/css">
</head>
<body>
	<div id="main">
		<h1>PharosFaces Login</h1>
		<div id="login">
			<h2>Login</h2>
			<form action="" method="post">
				<label>Username :</label>
				<input id="name" name="username" placeholder="Insert username here" type="text">
				<label>Password :</label>
				<input id="password" name="password" placeholder="Insert password here" type="password">
				<input class="btn orange login-btn" name="submit" type="submit" value=" Login ">
				<span><?php echo $error; ?></span>
			</form>
		</div>
	</div>
</body>
</html>