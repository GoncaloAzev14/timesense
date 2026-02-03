docker kill timesense
docker build -t local:timesense .
docker run "-p8080:80" "-p8443:443" "-p5432:5432" --rm --name timesense local:timesense
