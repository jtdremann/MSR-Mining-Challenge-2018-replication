# Install needed packages ahead of time
if (!require("pacman")) install.packages("pacman")
pacman::p_load(plyr, ggplot2, reshape2)

#Read in logs
logs <- read.csv("continuous-output2.csv")

#Order the logs by day of week
logs$orderedDaysOfWeek<-factor(logs$dayOfWeek,levels=levels(logs$dayOfWeek)[c(2, 6, 7, 5, 1, 3, 4)])

#Plot the total events for each day of the week
plot(logs$orderedDaysOfWeek)

#Calculate success and failure rate
logs$successRate <- ( (logs$buildTargetSuccesses + logs$testCaseSuccesses) / (logs$totalBuildTargets + logs$totalTestCaseResults) * 100 )

logs$failureRate <- ( (logs$buildTargetFails + logs$testCaseErrors + logs$testCaseFails) / (logs$totalBuildTargets + logs$totalTestCaseResults) * 100 )

#Create new df for non NaN success rates
sRate = logs[is.nan(logs$successRate) == FALSE,]

#Aggregate the success data
agg_success <- aggregate(x=list(Success_Rate=sRate$successRate),
                         by=list(Day=sRate$orderedDaysOfWeek),
                         FUN=mean)

#Aggregate the failure data
agg_failure <- aggregate(x=list(Failure_Rate=sRate$failureRate),
                         by=list(Day=sRate$orderedDaysOfWeek),
                         FUN=mean)

#Adding the failure data to the success data DF 
agg_success$Failure_Rate = agg_failure$Failure_Rate

# Basic line plot of both the success rate and failure rate
ggplot(data=agg_success, aes(x=Day, y=Success_Rate, group=1)) +
  geom_line() + aes(y = Success_Rate) + geom_line(aes(y = Failure_Rate)) +
  ylim(0, 100)

