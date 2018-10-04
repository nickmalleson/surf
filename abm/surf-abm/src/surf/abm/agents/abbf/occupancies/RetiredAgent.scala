package surf.abm.agents.abbf.occupancies

import surf.abm.agents.abbf.{ABBFAgent, TimeProfile}
import surf.abm.agents.abbf.activities._
import surf.abm.environment.Building
import surf.abm.main.{SurfABM, SurfGeometry}

/**
  * Created by geotcr on 28/09/2018.
  */
class RetiredAgent (override val state:SurfABM, override val home:SurfGeometry[Building])
  extends ABBFAgent(state, home) {

    def defineActivities(): Unit = {

      val tempActivities = scala.collection.mutable.Set[Activity]()

      // Definition of random numbers
      val rnd = state.random.nextDouble() * 4d
      // A random number between 0 and 4
      val rndLunchPreference = state.random.nextDouble() / 4d
      // Test with a random preference for eating and leisure activities. Should become activity specific.
      val rndDinnerPreference = state.random.nextDouble() // maybe not good because should mainly be at random day in week, not only done by random agents regularly
      val rndGoingOutPreference = state.random.nextDouble() / 10d
      val rndSportPreference = state.random.nextDouble() / 2d


      // SHOPPING
      val shoppingTimeProfile = TimeProfile(Array((7d, 0d),(10d + rnd/4d, 1d),(14d + rnd, 0d)))
      val shoppingActivity = ShopActivity(timeProfile = shoppingTimeProfile, agent = this, state)
      tempActivities += shoppingActivity

      // LUNCHING
      val lunchTimeProfile = TimeProfile(Array((11d, 0d), (11.5 + rnd/2d, rndLunchPreference), (12d + rnd/2d, rndLunchPreference), (15d, 0d)))
      val lunchActivity = LunchActivity(timeProfile = lunchTimeProfile, agent = this, state)
      tempActivities += lunchActivity

      // DINNER
      val dinnerTimeProfile = TimeProfile(Array((17d, 0d), (18d + rnd/2d, rndDinnerPreference), (19.5 + rnd/2d, rndDinnerPreference), (22.5, 0d)))
      val dinnerActivity = DinnerActivity(timeProfile = dinnerTimeProfile, agent = this, state)
      tempActivities += dinnerActivity

      // GOING OUT
      val goingOutTimeProfile = TimeProfile(Array((0d, rndGoingOutPreference / 4d), (2d, 0d), (20d, 0d), (22d, rndGoingOutPreference)))
      val goingOutActivity = GoingOutActivity(timeProfile = goingOutTimeProfile, agent = this, state)
      tempActivities += goingOutActivity

      // SPORTS
      val sportTimeProfile = TimeProfile(Array((7d, 0d), (10d + rnd, rndSportPreference), (18d, 0d)))
      val sportActivity = SportActivity(timeProfile = sportTimeProfile, agent = this, state)
      tempActivities += sportActivity

      // SLEEPING
      val atHomeActivity = SleepActivity(TimeProfile(Array((0d, 1d), (6d, 1d), (12d, 0.5), (20d, 1d))), agent = this)
      // Increase this activity to make it the most powerful activity to begin with, but with a bit of randomness
      // (repeatedly call the ++ function to increase it)
      for (i <- 1.until( (36 + rnd).toInt) ) {
        atHomeActivity.++()
      }
      tempActivities += atHomeActivity


      // Add these activities to the agent's activity list. At Home is the strongest initially.
      //val activities = Set[Activity](workActivity , shoppingActivity , atHomeActivity , lunchActivity , dinnerActivity, goingOutActivity )

      // Finally tell the agent about them
      //a.activities = activities
      this.activities ++= tempActivities

    }
  }

  object RetiredAgent {
    def apply(state: SurfABM, home: SurfGeometry[Building]): RetiredAgent =
      new RetiredAgent(state, home )



  }

