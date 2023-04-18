def return_up_outliers_IQR(series, threshold, verbose=False):
    """ Returns the outliers of the series.
    Outliers are detected with interquantile range.
    Quantile used are the threshold-th and (1-threshold)-th ones."""

    quantile_inf = series.quantile(threshold)
    quantile_sup = series.quantile(1-threshold)
    IQR = quantile_sup - quantile_inf
    outliers = series[(series > quantile_sup + 1.5 * IQR)]

    if verbose:
        print("For series", series.name, ": ")
        print("With quantile inf", threshold, ": {:.2f}".format(quantile_inf))
        print("and with quantile sup", (1-threshold), ": {:.2f}".format(quantile_sup))
        print("giving IQR :", IQR)
        print("Number of outliers :", len(outliers))

    return outliers

def return_up_outliers_Zscore(series, threshold, verbose=False):
    """ Returns the outliers of the series.
    Outliers are detected with z score function > threshold."""
    
    outliers = series[ series.add(-series.mean()).multiply(1/(series.std()+0.00000000001)) > threshold ]
    
    if verbose:
        print("For series", series.name, ": ")
        print("With Z score >", threshold, 
              "number of outliers :", len(outliers))

    return outliers