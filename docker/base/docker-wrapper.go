package main

import (
	"bufio"
	"fmt"
	"os"
	"regexp"
	"strings"
	"syscall"
)

func main() {
	currentCmd := make([]string, 0)
	currentCmd = append(currentCmd, "/usr/bin/docker")

	args := os.Args[1:]
	skipNext := false
	replaceNet := false

	for _, v := range args {
		if skipNext == true {
			skipNext = false
			continue
		}
		if v == "run" {
			replaceNet = true
			currentCmd = append(currentCmd, "run")
			currentCmd = append(currentCmd, "--net")
			currentCmd = append(currentCmd, fmt.Sprintf("container:%s", getContainerId()))
			continue
		}
		if v == "--net" && replaceNet == true {
			skipNext = true
			continue
		} else {
			currentCmd = append(currentCmd, v)
		}
	}
	syscall.Exec(currentCmd[0], currentCmd, os.Environ())
}

func getContainerId() string {
	myf, _ := os.Open("/etc/hosts")
	defer myf.Close()
	scnr := bufio.NewScanner(myf)
	lastLine := ""
	for scnr.Scan() {
		if len(strings.TrimSpace(scnr.Text())) == 0 {
			continue
		}
		lastLine = scnr.Text()

	}
	r := regexp.MustCompile(".*?[\\t\\s]+(.*)")
	return r.ReplaceAllString(lastLine, "$1")
}
