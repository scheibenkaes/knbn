clean:
	lein cljsbuild clean

prod:
	lein cljsbuild once prod

deploy: clean prod
	rsync -arv resources/public/* scheibenkaes.org:/home/b6n/web_apps/knbn
