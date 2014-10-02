build:
	lein cljsbuild once

deploy: build
	rsync -arv resources/public/* scheibenkaes.org:/home/b6n/web_apps/knbn
