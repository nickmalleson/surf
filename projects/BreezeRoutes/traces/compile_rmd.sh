Rscript -e "require(knitr) ; require(markdown) ;knit('analyse_traces.Rmd', 'analyse_traces.md'); markdownToHTML('analyse_traces.md', 'analys
e_traces.html');"

rm analyse_traces.md
