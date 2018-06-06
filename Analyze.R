# Install needed packages ahead of time
if (!require("pacman")) install.packages("pacman")
pacman::p_load(plyr, ggplot2, reshape2)

#Read in logs
dow <- read.csv("day-of-week-output.csv")
cont <- read.csv("continuous-output.csv")

#Order the logs by day of week
dow$orderedDaysOfWeek<-factor(dow$dayOfWeek,levels=levels(dow$dayOfWeek)[c(2, 6, 7, 5, 1, 3, 4)])
cont$orderedDaysOfWeek<-factor(cont$dayOfWeek,levels=levels(cont$dayOfWeek)[c(2, 6, 7, 5, 1, 3, 4)])

#Plot the total events for each day of the week
plot(dow$orderedDaysOfWeek)

#Calculate success and failure rate
agg <- aggregate(x=list(TBT=dow$totalBuildTargets, TBS=dow$buildTargetSuccesses, TBF=dow$buildTargetFails,
                        TTR=dow$totalTestCaseResults, TCS=dow$testCaseSuccesses, TCF=dow$testCaseFails, TCE=dow$testCaseErrors),
                 by=list(Day=dow$orderedDaysOfWeek),
                 FUN=sum)

agg$successRate <- ((agg$TBS + agg$TCS)/(agg$TBT + agg$TTR)) * 100
agg$failureRate <- ((agg$TBF + agg$TCF + agg$TCE)/(agg$TBT + agg$TTR)) * 100
dow$successRate <- ( (dow$buildTargetSuccesses + dow$testCaseSuccesses) / (dow$totalBuildTargets + dow$totalTestCaseResults) * 100 )
cont$successRate <- ( (cont$buildTargetSuccesses + cont$testCaseSuccesses) / (cont$totalBuildTargets + cont$totalTestCaseResults) * 100 )

dow$failureRate <- ( (dow$buildTargetFails + dow$testCaseErrors + dow$testCaseFails) / (dow$totalBuildTargets + dow$totalTestCaseResults) * 100 )
cont$failureRate <- ( (cont$buildTargetFails + cont$testCaseErrors + cont$testCaseFails) / (cont$totalBuildTargets + cont$totalTestCaseResults) * 100 )

#Create new df for non NaN success rates
dowRates = dow[is.nan(dow$successRate) == FALSE,]
contRates = cont[is.nan(cont$successRate) == FALSE,]

#Aggregate the success data
agg_success <- aggregate(x=list(Success_Rate=dowRates$successRate, Failure_Rate=dowRates$failureRate),
                         by=list(Day=dowRates$orderedDaysOfWeek),
                         FUN=mean)

# Basic line plot of both the success rate and failure rate
ggplot(data=agg, aes(x=Day, y=successRate, group=1)) +
  geom_line() + aes(y = successRate) + geom_line(aes(y = failureRate)) +
  ylim(0, 100)

# ggplot(data=agg_success, aes(x=Day, y=Success_Rate, group=1)) +
  # geom_line() + aes(y = Success_Rate) + geom_line(aes(y = Failure_Rate)) +
  # ylim(0, 100)

# Continuous time data
dowAggTime <- aggregate(x=list(Time=dow$continuousTime),
                        by=list(Day=dow$orderedDaysOfWeek),
                        FUN=sum)
dowAggTime$Hours = dowAggTime$Time / 3600
ggplot(data=dowAggTime, aes(x=Day, y=Hours)) + geom_boxplot() + ylim(0,500)



# Continuous Time vs Success Rate
range1 <- contRates[contRates$continuousTime < (25*60),]
range2 <- contRates[contRates$continuousTime >= (25*60) & contRates$continuousTime < (60*60),]
range3 <- contRates[contRates$continuousTime >= (60*60) & contRates$continuousTime < (120*60),]
range4 <- contRates[contRates$continuousTime >= (120*60),]

rSuccesses = c(mean(range1$successRate),mean(range2$successRate),mean(range3$successRate),mean(range4$successRate))
rFailures = c(mean(range1$failureRate),mean(range2$failureRate),mean(range3$failureRate),mean(range4$failureRate))
rTitles = c("0-25m", "25m-1hr", "1hr-2hr", "2+hr")

intervals = data.frame(Duration=rTitles, SuccessRate=rSuccesses, FailRate=rFailures)
intervals$orderedDuration <- factor(intervals$Duration, levels=levels(intervals$Duration)[c(1,4,2,3)])

ggplot(data=intervals, aes(x=orderedDuration, y=SuccessRate, group=1)) +
  geom_line() + aes(y = SuccessRate) + geom_line(aes(y = FailRate)) +
  ylim(0, 100)

contRates$duration = ""
contRates[contRates$continuousTime < (25*60),"duration"] = "0m-25m"
contRates[contRates$continuousTime >= (25*60) & contRates$continuousTime < (60*60),"duration"] = "25m-1h"
contRates[contRates$continuousTime >= (60*60) & contRates$continuousTime < (120*60),"duration"] = "1h-2h"
contRates[contRates$continuousTime >= (120*60),"duration"] = "2h+"

myTest = table(contRates$dayOfWeek, contRates$duration)
chisq.test(myTest)
