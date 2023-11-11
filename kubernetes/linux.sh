curl -sfL https://get-kk.kubesphere.io | VERSION=v1.2.1 sh -
chmod +x kk
sudo ./kk create config
sudo vi config-sample.yaml