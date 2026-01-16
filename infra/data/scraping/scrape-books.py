import  requests
from bs4 import BeautifulSoup
import pandas as pd
import os


print(" Téléchargement de la page")


url = "https://books.toscrape.com/catalogue/page-1.html"
response = requests.get(url)


if response.status_code != 200:
    print(f" Erreur HTTP : {response.status_code}")
    exit()


print("Page chargée ! Analyse HTML")


soup = BeautifulSoup(response.text, "html.parser")
books = soup.find_all("article", class_="product_pod")


titles, prices, stocks = [], [], []


for b in books:
    title = b.h3.a["title"]
    price = b.find("p", class_="price_color").text.strip()
    stock = b.find("p", class_="instock availability").text.strip()


    titles.append(title)
    prices.append(price)
    stocks.append(stock)


df = pd.DataFrame({
    "title": titles,
    "price": prices,
    "stock": stocks
})


#SAUVEGARDE LOCALE
local_path = "/home/jsa/scraping/books.csv"
df.to_csv(local_path, index=False, encoding="utf-8")


print(f" Fichier local créé : {local_path}")


#ENVOI VERS HDFS (Raw Zone)
print(" Envoi du fichier dans HDFS")


hdfs_raw_path = "/data/raw/webscraping"
# création du dossier dans HDFS
os.system(f"hdfs dfs -mkdir -p {hdfs_raw_path}")


# upload du fichier
os.system(f"hdfs dfs -put -f {local_path} {hdfs_raw_path}/books.csv")


print(" Upload terminé")
print(f" Fichier disponible dans HDFS : {hdfs_raw_path}/books.csv")