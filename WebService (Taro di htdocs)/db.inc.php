<?php
	$db_host = 'localhost';
	$db_name = 'pharos';
	$db_user = 'root';
	$db_pass = '';
	
	if(!($link = @mysql_connect($db_host, $db_user, $db_pass))){
		die('Gagal terhubung ke MySQL: ' . mysql_error());
	}
	
	if(!($db = mysql_select_db($db_name)))
	{
		die('Gagal memilih database.');
	}
?>