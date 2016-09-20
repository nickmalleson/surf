# REQUIRES package readxl

setwd("M:/electoral/")

# Males
remove(list=ls())

L.df<-read_excel("Copy of SAPE17DT10a-mid-2014-coa-unformatted-syoa-estimates-london.xlsx",sheet="Mid-2014 Males")
NW.df<-read_excel("Copy of SAPE17DT10b-mid-2014-coa-unformatted-syoa-estimates-north-west.xlsx",sheet="Mid-2014 Males")
YH.df<-read_excel("Copy of SAPE17DT10c-mid-2014-coa-unformatted-syoa-estimates-yorkshire-and-the-humber.xlsx",sheet="Mid-2014 Males")
NE.df<-read_excel("Copy of SAPE17DT10d-mid-2014-coa-unformatted-syoa-estimates-north-east.xlsx",sheet="Mid-2014 Males")
WM.df<-read_excel("Copy of SAPE17DT10e-mid-2014-coa-unformatted-syoa-estimates-west-midlands.xlsx",sheet="Mid-2014 Males")
EM.df<-read_excel("Copy of SAPE17DT10f-mid-2014-coa-unformatted-syoa-estimates-east-midlands.xlsx",sheet="Mid-2014 Males")
SW.df<-read_excel("Copy of SAPE17DT10g-mid-2014-coa-unformatted-syoa-estimates-south-west.xlsx",sheet="Mid-2014 Males")
E.df<-read_excel("Copy of SAPE17DT10h-mid-2014-coa-unformatted-syoa-estimates-east.xlsx",sheet="Mid-2014 Males")
SE.df<-read_excel("Copy of SAPE17DT10i-mid-2014-coa-unformatted-syoa-estimates-south-east.xlsx",sheet="Mid-2014 Males")
W.df<-read_excel("Copy of SAPE17DT10j-mid-2014-coa-unformatted-syoa-estimates-wales.xlsx",sheet="Mid-2014 Males")

# England and Wales
EW.df<-rbind(L.df,NW.df,YH.df,NE.df,WM.df,EM.df,SW.df,E.df,SE.df,W.df)

remove(L.df)
remove(NW.df)
remove(YH.df)
remove(NE.df)
remove(WM.df)
remove(EM.df)
remove(SW.df)
remove(E.df)
remove(SE.df)
remove(W.df)

# Population Weighted Centroid
PWC.df<-read_excel("PWC_in_EW.xlsx",sheet="PWC_in_E")
PWC_pop.df<-merge(PWC.df,EW.df,by.x="OA11CD",by.y="OA11CD")

# Westminster Parliamentry Constituency
WPC_pop.df<-aggregate(PWC_pop.df[,6:97],by=list(PWC_pop.df$Name),FUN=sum)

write.csv(WPC_pop.df,"PC_males.csv")

# Females
remove(list=ls())

L.df<-read_excel("Copy of SAPE17DT10a-mid-2014-coa-unformatted-syoa-estimates-london.xlsx",sheet="Mid-2014 Females")
NW.df<-read_excel("Copy of SAPE17DT10b-mid-2014-coa-unformatted-syoa-estimates-north-west.xlsx",sheet="Mid-2014 Females")
YH.df<-read_excel("Copy of SAPE17DT10c-mid-2014-coa-unformatted-syoa-estimates-yorkshire-and-the-humber.xlsx",sheet="Mid-2014 Females")
NE.df<-read_excel("Copy of SAPE17DT10d-mid-2014-coa-unformatted-syoa-estimates-north-east.xlsx",sheet="Mid-2014 Females")
WM.df<-read_excel("Copy of SAPE17DT10e-mid-2014-coa-unformatted-syoa-estimates-west-midlands.xlsx",sheet="Mid-2014 Females")
EM.df<-read_excel("Copy of SAPE17DT10f-mid-2014-coa-unformatted-syoa-estimates-east-midlands.xlsx",sheet="Mid-2014 Females")
SW.df<-read_excel("Copy of SAPE17DT10g-mid-2014-coa-unformatted-syoa-estimates-south-west.xlsx",sheet="Mid-2014 Females")
E.df<-read_excel("Copy of SAPE17DT10h-mid-2014-coa-unformatted-syoa-estimates-east.xlsx",sheet="Mid-2014 Females")
SE.df<-read_excel("Copy of SAPE17DT10i-mid-2014-coa-unformatted-syoa-estimates-south-east.xlsx",sheet="Mid-2014 Females")
W.df<-read_excel("Copy of SAPE17DT10j-mid-2014-coa-unformatted-syoa-estimates-wales.xlsx",sheet="Mid-2014 Females")

EW.df<-rbind(L.df,NW.df,YH.df,NE.df,WM.df,EM.df,SW.df,E.df,SE.df,W.df)

remove(L.df)
remove(NW.df)
remove(YH.df)
remove(NE.df)
remove(WM.df)
remove(EM.df)
remove(SW.df)
remove(E.df)
remove(SE.df)
remove(W.df)

PWC.df<-read_excel("PWC_in_EW.xlsx",sheet="PWC_in_E")
PWC_pop.df<-merge(PWC.df,EW.df,by.x="OA11CD",by.y="OA11CD")

WPC_pop.df<-aggregate(PWC_pop.df[,6:97],by=list(PWC_pop.df$Name),FUN=sum)

write.csv(WPC_pop.df,"PC_females.csv")
