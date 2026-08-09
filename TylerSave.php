<?php

    //
    // TylerSave.php
    //
    // Implements saving and loading.
    // Currently anyone can read and clobber anyone else's files.
    //
    // NOTE: the directory TylerSave.php.dir should exist and be
    // writable (but not necessarily readable)
    // by the user apache (e.g. owned by you with mode 0777).
    //
    // Last modified: Fri Jun 14 14:14:58 PDT 2002
    //

    //
    // Legal requests:
    //    0. Given $login,$password,
    //         list the saved files.
    //    1. Given $login,$password,$saveFileName,$saveFileContents,
    //         save it.
    //    2. Given $login,$password,$loadFileName,
    //         load it.
    //

    // This is from // http://php.weblogs.com/stories/storyReader$465 .
    // if we don't do this, all GET/POST/COOKIE strings will have
    // backslashes added to them, which is really stupid.
    if (get_magic_quotes_gpc()){ 
        while (list ($key, $val) = each ($HTTP_POST_VARS))
            $$key = stripslashes($val); 
        while (list ($key, $val) = each ($HTTP_GET_VARS))
            $$key = stripslashes($val);
    }

    $saveDir = "$SCRIPT_FILENAME.dir"; // XXX cop-out

    if (!empty($login) && !empty($password))
    {
        $loginSubdir = $saveDir . "/login=" . urlencode($login);
        $passwordSubdir = $loginSubdir . "/password=" . urlencode($password);

        //echo "loginSubdir=\"$loginSubdir\"\n<br>";
        //echo "passwordSubdir=\"$passwordSubdir\"\n<br>";

        if (!empty($saveFileName) && !empty($saveFileContents))
        {
            // re-urlencode the file name,
            // to avoid security issues of .., slashes, etc.
            $saveFileName = urlencode($saveFileName);

            // Don't want to see warnings...
            $old_level = error_reporting();
            //error_reporting($old_level & ~E_WARNING);

                umask(0000); // otherwise created dirs and files will come out 0755

                if (!file_exists("$saveDir/index.htm"))
                    touch("$saveDir/index.htm"); // prevent dir browsing
                if (!file_exists($loginSubdir))
                    mkdir($loginSubdir, 0777); // XXX should check for failure
                if (!file_exists("$loginSubdir/index.htm"))
                    touch("$loginSubdir/index.htm"); // prevent dir browsing
                if (!file_exists($passwordSubdir))
                    mkdir($passwordSubdir, 0777); // XXX should check for failure
                if (!file_exists("$passwordSubdir/index.htm"))
                    touch("$passwordSubdir/index.htm"); // prevent dir browsing

                $fp = fopen("$passwordSubdir/fileName=$saveFileName", "w");
                // umask didn't seem to do it, so do it explicitly...
                chmod("$passwordSubdir/fileName=$saveFileName", 0666);

            // Restore warnings...
            error_reporting($old_level);

            if ($fp != NULL)
            {
                fwrite($fp, $saveFileContents); // XXX should check for failure
                fclose($fp); // XXX should check for failure
                echo "SUCCESS\n";
            }
            else
                echo "FAIL\n";
        }
        else if (!empty($loadFileName))
        {
            // re-urlencode the filename,
            // to avoid security issues of .., slashes, etc.
            $loadFileName = urlencode($loadFileName);

            // Don't want to see warnings...
            $old_level = error_reporting();
            error_reporting($old_level & ~E_WARNING);

                readfile("$passwordSubdir/fileName=$loadFileName"); // XXX should check for failure

            // Restore warnings...
            error_reporting($old_level);
        }
        else // list all files under this login/password
        {
            if (file_exists($passwordSubdir))
            {
                // Don't want to see warnings...
                $old_level = error_reporting();
                error_reporting($old_level & ~E_WARNING);

                    $dp = opendir($passwordSubdir);

                // Restore warnings...
                error_reporting($old_level);

                if ($dp != null)
                {
                    while (($fileName = readdir($dp)) !== false)
                    {
                        if (ereg("^fileName=",$fileName))
                        {
                            $fileName = ereg_replace("^fileName=","",$fileName);
                            echo "$fileName\n"; // keep urlencoded
                        }
                    }
                    closedir($dp);
                }
            }
        }
    } // if (!empty(login) && !empty(password))
    else
    {
        echo "huh?\n";
    }
?>
