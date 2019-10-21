FROM python:slim-buster
MAINTAINER t.joedden@gmail.com

ADD zeroops .
RUN pip3 install -r requirements.txt

ENTRYPOINT ["python", "LocalMain.py"]