getWindData <- function(underlying, startDate, endDate) {
    if(!("WindR" %in% .packages())) {
        require(WindR)
    }
    if(!w.isconnected()) {
        w.start(showmenu = FALSE)
    }
    return(w.wsd(underlying,"open,high,low,close",startDate,endDate,"PriceAdj=F"))
}
