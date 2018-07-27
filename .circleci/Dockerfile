FROM circleci/openjdk:8-jdk

COPY private_key.asc.enc /private_key.asc.enc
COPY public_key.asc.enc /public_key.asc.enc

RUN sudo apt-get install gnupg2 -y

CMD mkdir ${HOME}/.gnupg | \
    openssl aes-256-cbc -d -in /public_key.asc.enc -out ${HOME}/.gnupg/pubring.gpg -k ${ENC_PASS} | \
    openssl aes-256-cbc -d -in /private_key.asc.enc -out ${HOME}/.gnupg/secring.gpg -k ${ENC_PASS} | \
    tail -f /dev/null