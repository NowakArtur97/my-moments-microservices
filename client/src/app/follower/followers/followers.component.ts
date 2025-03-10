import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from 'src/app/auth/services/user.service';

import UserAcquaintance from '../models/user-acquaintance.model';
import { FollowerService } from '../service/follower.service';
import { UserAcquaintancesComponent } from '../user-acquaintances/user-acquaintances.component';

@Component({
  selector: 'app-followers',
  templateUrl: '../user-acquaintances/user-acquaintances.component.html',
  styleUrls: ['../user-acquaintances/user-acquaintances.component.css'],
})
export class FollowersComponent
  extends UserAcquaintancesComponent
  implements OnInit {
  private followers: UserAcquaintance[] = [];

  constructor(
    protected followerService: FollowerService,
    protected userService: UserService,
    protected router: Router
  ) {
    super(followerService, userService, router);
    this.subject = this.followerService.myFollowers;
  }

  ngOnInit(): void {
    this.followerService.myFollowers.subscribe((followers) => {
      this.followers = followers;
      if (this.users.length !== 0) {
        this.usersLoaded = this.followers;
        return;
      }
      this.loadUsers(this.followers);
    });
  }
}
