build:
	lein cljsbuild once

deploy: build
	rsync -arv css/ fonts/ js/ out/ index.html knbn.js main.css scheibenkaes.org:/home/b6n/web_apps/knbn
