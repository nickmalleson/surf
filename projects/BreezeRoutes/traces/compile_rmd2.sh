Rscript -e "require(knitr) ; require(markdown) ;knit('analyse_traces2.Rmd', 'analyse_traces2.md'); markdownToHTML('analyse_traces.md', 'analys
e_traces2.html');"

rm analyse_traces2.md
